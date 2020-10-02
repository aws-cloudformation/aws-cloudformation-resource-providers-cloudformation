package software.amazon.cloudformation.resourceversion;

import com.amazonaws.util.StringUtils;
import com.sun.javafx.binding.StringFormatter;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
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

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
            proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        return initiator
            .translateToServiceRequest(Translator::translateToCreateRequest)
            .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::registerType))
            .done((registerTypeRequest, registerTypeResponse, sdkProxyClient, model, cc) -> {
                cc.setRegistrationToken(registerTypeResponse.registrationToken());
                return predictArn(request, initiator);
            })
            .then(progress -> stabilizeOnCreate(initiator, progress.getCallbackContext().getRegistrationToken()))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, progress.getCallbackContext(), proxyClient, logger));
    }

    /**
     * To ensure we can return a primaryIdentifier for this type within the 60s timeout required,
     * we must predict the Arn which this type will be registered with. There may be some edge case
     * issues here, which we can fix by moving this prediction to the Registry Service itself
     */
    ProgressEvent<ResourceModel, CallbackContext> predictArn(
        final ResourceHandlerRequest<ResourceModel> request,
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator) {

        final ResourceModel model = initiator.getResourceModel();
        final CallbackContext context = initiator.getCallbackContext();
        context.setDeprecatedStatus(DeprecatedStatus.LIVE);
        CallChain.Initiator<CloudFormationClient, ListTypeVersionsResponse, CallbackContext>
            listApi = initiator.rebindModel(ListTypeVersionsResponse.builder().build());

        TypeVersionSummary latest = getPredictedSummary(request, model);
        do {
            ProgressEvent<ListTypeVersionsResponse, CallbackContext> response =
                listApi.translateToServiceRequest(incoming ->
                    ListTypeVersionsRequest.builder()
                        .deprecatedStatus(context.getDeprecatedStatus())
                        .nextToken(incoming.nextToken())
                        .typeName(model.getTypeName())
                        .type(RegistryType.RESOURCE).build())
                    .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::listTypeVersions))
                    .handleError(
                        (request_, exception, client, model_, context_) -> {
                            if (exception instanceof CfnRegistryException) {
                                // registration can be assumed to be the first version for a type
                                return ProgressEvent.success(
                                    ListTypeVersionsResponse.builder().typeVersionSummaries(
                                        getPredictedSummary(request, model)
                                    ).build(),
                                    context
                                );
                            }
                            throw exception;
                        }
                    )
                    .done(response_ -> ProgressEvent.success(response_, context));

            if (!response.isSuccess()) {
                return ProgressEvent.failed(
                    model, context, response.getErrorCode(), response.getMessage());
            }

            ListTypeVersionsResponse callResponse = response.getResourceModel();
            final TypeVersionSummary currentLatest = latest;
            latest = callResponse.typeVersionSummaries().stream().reduce(
                (summary, next) -> {
                    int first = getVersion(summary.arn());
                    int second = getVersion(next.arn());
                    int latestVersion = getVersion(currentLatest.arn());
                    if (second > first) {
                        return latestVersion < second ? next : currentLatest;
                    } else {
                        return latestVersion < first ? summary : currentLatest;
                    }
                }
            ).orElse(latest);

            if (callResponse.nextToken() != null) {
                listApi = listApi.rebindModel(callResponse);
                continue;
            } else {
                if (context.getDeprecatedStatus() == DeprecatedStatus.LIVE) {
                    // now we need to check any deprecated versions as well
                    context.setDeprecatedStatus(DeprecatedStatus.DEPRECATED);
                    listApi = listApi.rebindModel(callResponse);
                    continue;
                }
            }

            String arn = latest.arn();
            Integer current = Integer.valueOf(arn.substring(arn.lastIndexOf("/") + 1));
            arn = arn.substring(0, arn.lastIndexOf("/") + 1)
                .concat(String.format("%08d", current + 1));
            if(StringUtils.isNullOrEmpty(context.getPredictedArn())) {
                context.setPredictedArn(arn);
                model.setArn(arn);
            }
            return ProgressEvent.progress(model, context);

        } while (true);
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

    ProgressEvent<ResourceModel, CallbackContext> stabilizeOnCreate(
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator,
        final String registrationToken) {

        this.logger.log("Stabilizing Registration: " + initiator.getCallbackContext().getRegistrationToken());

        return initiator.initiate("stabilize")
            .translateToServiceRequest(m ->
                initiator.getCallbackContext().findAllRequestByContains("cloudformation:RegisterType").get(0))
            .makeServiceCall((r, c) ->
                initiator.getCallbackContext().findAllResponseByContains("cloudformation:RegisterType").get(0))
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
                    throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, initiator.getResourceModel().getArn());
                } else {
                    logger.log(String.format("Stabilization On Create failed with the status %s",response.progressStatusAsString()));
                    return false;
                }
            })
            .progress();
    }
}
