package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Optional;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;

        return proxy.initiate("AWS-CloudFormation-ResourceVersion::Create", proxyClient, resourceModel, context)
            .translateToServiceRequest(Translator::translateToCreateRequest)
            .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::registerType))
            .done((registerTypeRequest, registerTypeResponse, sdkProxyClient, model, cc) -> {
                cc.setRegistrationToken(registerTypeResponse.registrationToken());
                return predictArn(sdkProxyClient, request, cc);
            })
            .then(progress -> stabilizeOnCreate(proxy, proxyClient, progress))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, context, proxyClient, logger));
    }

    /**
     * To ensure we can return a primaryIdentifier for this type within the 60s timeout required,
     * we must predict the Arn which this type will be registered with. There may be some edge case
     * issues here, which we can fix by moving this prediction to the Registry Service itself
     */
    ProgressEvent<ResourceModel, CallbackContext> predictArn(
        final ProxyClient<CloudFormationClient> proxyClient,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext) {

        String arn;
        try {
            // find the highest existing version number for this type
            int highestExistingVersionNumber = 0;
            while (true) {
                final ListTypeVersionsRequest listTypeVersionsRequest = ListTypeVersionsRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(request.getDesiredResourceState().getTypeName())
                    .build();
                final ListTypeVersionsResponse response = proxyClient.injectCredentialsAndInvokeV2(listTypeVersionsRequest,
                    proxyClient.client()::listTypeVersions);

                final Optional<Integer> highestVersionInSet = response.typeVersionSummaries()
                    .stream()
                    .map(t -> Integer.parseInt(t.arn().substring(t.arn().lastIndexOf("/") + 1)))
                    .max(Integer::compareTo);

                highestExistingVersionNumber = Math.max(highestExistingVersionNumber, highestVersionInSet.get());

                if (response.nextToken() == null) {
                    // capture an Arn - they are all the same except the version Id which we'll replace ouside the loop
                    arn = response.typeVersionSummaries().get(0).arn();
                    break;
                }
            }

            // bump the version number for this revision
            arn = arn
                .substring(0, arn.lastIndexOf("/") + 1)
                .concat(String.format("%08d", highestExistingVersionNumber + 1));

        } catch (final TypeNotFoundException e) {
            // registration can be assumed to be the first version for a type
            arn = String.format("arn:%s:cloudformation:%s:%s:type/resource/%s/00000001",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                request.getDesiredResourceState().getTypeName().replace("::", "-"));
        } catch (final CfnRegistryException e) {
            logger.log(Arrays.toString(e.getStackTrace()));
            throw e;
        }

        callbackContext.setPredictedArn(arn);
        request.getDesiredResourceState().setArn(arn);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(callbackContext)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(request.getDesiredResourceState())
            .build();
    }

    ProgressEvent<ResourceModel, CallbackContext> stabilizeOnCreate(
        final AmazonWebServicesClientProxy proxy,
        final ProxyClient<CloudFormationClient> proxyClient,
        final ProgressEvent<ResourceModel, CallbackContext> progress) {

        final ResourceModel resourceModel = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();
        final String registrationToken = callbackContext.getRegistrationToken();

        return proxy.initiate("AWS-CloudFormation-ResourceVersion::StabilizeCreate", proxyClient, resourceModel, callbackContext)
            .translateToServiceRequest(awsRequest -> DescribeTypeRegistrationRequest.builder().registrationToken(registrationToken).build())
            .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::describeTypeRegistration))
            .stabilize((request, response, proxyInvocation, model, cc) -> {
                if (response.progressStatus().equals(RegistrationStatus.COMPLETE)) {
                    logger.log(String.format("%s registration successfully completed [%s].", ResourceModel.TYPE_NAME, response.typeVersionArn()));
                    return true;
                } else if (response.progressStatus().equals(RegistrationStatus.FAILED)) {
                    logger.log(String.format("Registration request %s failed with '%s'", registrationToken, response.description()));
                    throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, resourceModel.getArn());
                } else {
                    return false;
                }
            })
            .progress();
    }
}
