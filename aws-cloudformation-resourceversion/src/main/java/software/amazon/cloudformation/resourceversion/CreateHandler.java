package software.amazon.cloudformation.resourceversion;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        ResourceModel resourceModel = request.getDesiredResourceState();
        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate("create", proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::registerType))
                                .done((registerTypeRequest, registerTypeResponse, sdkProxyClient, model, cc) -> {
                                    cc.setRegistrationToken(registerTypeResponse.registrationToken());
                                    return predictArn(progress, proxyClient, request);
                                })
                )
                .then(progress -> stabilizeOnCreate(progress, proxy, proxyClient))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, progress.getCallbackContext(), proxyClient, logger));
    }

    /**
     * To ensure we can return a primaryIdentifier for this type within the 60s timeout required,
     * we must predict the Arn which this type will be registered with. There may be some edge case
     * issues here, which we can fix by moving this prediction to the Registry Service itself
     */
    ProgressEvent<ResourceModel, CallbackContext> predictArn(ProgressEvent<ResourceModel, CallbackContext> progress,
                                                             ProxyClient<CloudFormationClient> proxyClient,
                                                             ResourceHandlerRequest<ResourceModel> request) {
        CallbackContext context = progress.getCallbackContext();
        context.setDeprecatedStatus(DeprecatedStatus.LIVE);
        ResourceModel model = progress.getResourceModel();
        String marker = null;
        TypeVersionSummary latest = getPredictedSummary(request, model);
        do {
            //make a ListTypeVersions API call to get all versionIDs for status LIVE for the first time
            final ListTypeVersionsResponse listTypeVersionsResponse;

            try {
                listTypeVersionsResponse = proxyClient.injectCredentialsAndInvokeV2(
                        Translator.translateToListTypeVersionsRequest(model, marker, context.getDeprecatedStatus()), proxyClient.client()::listTypeVersions);
            } catch (CfnRegistryException e) {
                // registration can be assumed to be the first version for a type, the version will be 00000001.
                String arn = String.format("arn:%s:cloudformation:%s:%s:type/resource/%s/00000001",
                        request.getAwsPartition(),
                        request.getRegion(),
                        request.getAwsAccountId(),
                        model.getTypeName().replace("::", "-"));
                model.setArn(arn);
                return ProgressEvent.success(model, context);
            }

            final TypeVersionSummary currentLatest = latest;
            latest = getLatestVersion(latest, currentLatest, listTypeVersionsResponse);
            marker = listTypeVersionsResponse.nextToken();
            if(!StringUtils.isNullOrEmpty(marker)) {
                continue;
            }
            else {
                if (context.getDeprecatedStatus() == DeprecatedStatus.LIVE) {
                    logger.log("changing the status from LIVE to DEPRECATED"); //We need to know the highest version ID for deprecated versions as well to predict the arn.
                    context.setDeprecatedStatus(DeprecatedStatus.DEPRECATED);
                    continue;
                }
            }
            String arn = getCurrentArn(latest, currentLatest); //returns the largest version ID +1
            if (StringUtils.isNullOrEmpty(context.getPredictedArn())) {
                context.setPredictedArn(arn);
                logger.log(String.format("The predicted arn for the resource %s is %s ", model.getTypeName(), arn));
                model.setArn(arn);
            }
            progress.setResourceModel(model);
            return progress;
        } while (true);
    }

    private String getCurrentArn(TypeVersionSummary latest, TypeVersionSummary currentLatest) {
        String arn = latest.arn();
        String currentArn = currentLatest.arn();
        Integer currentLatestVersion = Integer.valueOf(currentArn.substring(currentArn.lastIndexOf("/") + 1));
        Integer latestVersion = Integer.valueOf(arn.substring(arn.lastIndexOf("/") + 1));
        Integer current = currentLatestVersion > latestVersion ? currentLatestVersion : latestVersion; // to get the latest(largest) version ID
        arn = arn.substring(0, arn.lastIndexOf("/") + 1)
                .concat(String.format("%08d", current + 1));
        return arn;
    }

    private TypeVersionSummary getLatestVersion(TypeVersionSummary latest, TypeVersionSummary currentLatest, ListTypeVersionsResponse listTypeVersionsResponse) {
        return listTypeVersionsResponse.typeVersionSummaries().stream().reduce(
                (summary, next) -> {
                    int first = getVersion(summary.arn());
                    int second = getVersion(next.arn());
                    int latestVersion = getVersion(currentLatest.arn());
                    logger.log("first second and latest is "+first+" "+second+" "+latestVersion);
                    if (second > first) {
                        return latestVersion < second ? next : currentLatest;
                    } else {
                        return latestVersion < first ? summary : currentLatest;
                    }
                }
        ).orElse(latest);
    }

    private TypeVersionSummary getPredictedSummary(
            final ResourceHandlerRequest<ResourceModel> request,
            final ResourceModel model) {

        // registration can be assumed to be the first version for a type
        String arn = String.format("arn:%s:cloudformation:%s:%s:type/resource/%s/00000000",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                model.getTypeName().replace("::", "-"));
        return TypeVersionSummary.builder()
                .arn(arn)
                .versionId("0000000")
                .typeName(model.getTypeName())
                .type(RegistryType.RESOURCE)
                .build();
    }

    private int getVersion(String arn) {
        return Integer.parseInt(arn.substring(arn.lastIndexOf("/") + 1));
    }

    ProgressEvent<ResourceModel, CallbackContext> stabilizeOnCreate(ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                    AmazonWebServicesClientProxy proxy,
                                                                    ProxyClient<CloudFormationClient> proxyClient) {

        this.logger.log("Stabilizing Registration: " + progress.getCallbackContext().getRegistrationToken());
        String registrationToken = progress.getCallbackContext().getRegistrationToken();
        return proxy.initiate("stabilize", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(m ->
                        progress.getCallbackContext().findAllRequestByContains("cloudformation:RegisterType").get(0))
                .makeServiceCall((r, c) ->
                        progress.getCallbackContext().findAllResponseByContains("cloudformation:RegisterType").get(0))
                .stabilize((awsRequest, awsResponse, proxyInvocation, model, cc) -> {
                    DescribeTypeRegistrationRequest describe =
                            DescribeTypeRegistrationRequest.builder()
                                    .registrationToken(registrationToken)
                                    .build();
                    DescribeTypeRegistrationResponse response =
                            proxyInvocation.injectCredentialsAndInvokeV2(describe, proxyInvocation.client()::describeTypeRegistration);
                    if (response.progressStatus().equals(RegistrationStatus.COMPLETE)) {
                        logger.log(String.format("%s registration successfully completed [%s].", ResourceModel.TYPE_NAME, response.typeVersionArn()));
                        return true;
                    } else if (response.progressStatus().equals(RegistrationStatus.FAILED)) {
                        logger.log(String.format("Registration request %s failed with '%s'", registrationToken, response.description()));
                        throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, progress.getResourceModel().getArn());
                    } else {
                        logger.log(String.format("Stabilization On Create failed with the status %s", response.progressStatusAsString()));
                        return false;
                    }
                })
                .progress();
    }
}
