package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
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
        logger.log(String.format("Creating the resource version for the type %s", resourceModel.getTypeName()));
        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-CloudFormation-ResourceVersion::Create", proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::registerType))
                                .done((registerTypeRequest, registerTypeResponse, sdkProxyClient, model, cc) -> {
                                            logger.log(String.format("The resource registered successfully. The registrationToken for the Type [%s] is %s", ResourceModel.TYPE_NAME, registerTypeResponse.registrationToken()));
                                            cc.setRegistrationToken(registerTypeResponse.registrationToken());
                                            DescribeTypeRegistrationResponse describeTypeRegistrationResponse =
                                                    proxy.injectCredentialsAndInvokeV2(Translator
                                                                    .translateToDescribeTypeRegistration(cc.getRegistrationToken()),
                                                            proxyClient.client()::describeTypeRegistration);
                                            if (describeTypeRegistrationResponse == null) {
                                                logger.log(String.format("Failed to describe registration status, invalid response, resource=%s arn=%s",
                                                        model.getTypeName(), model.getArn()));
                                                throw new CfnInternalFailureException();
                                            }
                                            logger.log(String.format("Fetching the TypeVersionArn [%s] from the DescribeTypeRegistrationResponse [%s] with the registration token [%s]", describeTypeRegistrationResponse.typeVersionArn(), describeTypeRegistrationResponse, cc.getRegistrationToken()));
                                            model.setArn(describeTypeRegistrationResponse.typeVersionArn());
                                            return ProgressEvent.progress(model, cc);
                                        }
                                )
                )
                .then(progress -> stabilizeOnCreate(progress, proxy, proxyClient))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, progress.getCallbackContext(), proxyClient, logger));
    }

    ProgressEvent<ResourceModel, CallbackContext> stabilizeOnCreate(ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                    AmazonWebServicesClientProxy proxy,
                                                                    ProxyClient<CloudFormationClient> proxyClient) {

        this.logger.log(String.format("Stabilizing Registration for the Arn [%s] with registration token [%s]",progress.getResourceModel().getArn(), progress.getCallbackContext().getRegistrationToken()));
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
