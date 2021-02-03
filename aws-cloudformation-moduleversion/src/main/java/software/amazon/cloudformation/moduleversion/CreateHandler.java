package software.amazon.cloudformation.moduleversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.Arrays;

public class CreateHandler extends BaseHandlerStd {

    private final ReadHandler readHandler;

    private static final Duration HANDLER_CALLBACK_DELAY_SECONDS = Duration.ofSeconds(15L);
    private static final Duration HANDLER_TIMEOUT_MINUTES = Duration.ofMinutes(30L);
    private static final Constant BACKOFF_STRATEGY = Constant.of().timeout(HANDLER_TIMEOUT_MINUTES).delay(HANDLER_CALLBACK_DELAY_SECONDS).build();

    public CreateHandler() {
        this(new ReadHandler());
    }

    CreateHandler(final ReadHandler readHandler) {
        this.readHandler = readHandler;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        validateModel(model);

        logger.log(String.format("Registering module version, module=%s", model.getModuleName()));
        return proxy.initiate("AWS-CloudFormation-ModuleVersion::Create", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .backoffDelay(BACKOFF_STRATEGY)
                .makeServiceCall((registerTypeRequest, client) -> {
                    final RegisterTypeResponse registerTypeResponse = registerModule(registerTypeRequest, client, model);
                    callbackContext.setRegistrationToken(registerTypeResponse.registrationToken());
                    return registerTypeResponse;
                })
                .stabilize(this::stabilize)
                .progress()
                .then(progress -> readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private RegisterTypeResponse registerModule(
            final RegisterTypeRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {
        RegisterTypeResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::registerType);
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to register module version, module=%s\n%s",
                    model.getModuleName(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }

    private DescribeTypeRegistrationResponse describeModuleRegistration(
            final DescribeTypeRegistrationRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {
        DescribeTypeRegistrationResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::describeTypeRegistration);
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to describe registration status of module version, module=%s\n%s",
                    model.getModuleName(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }

    private Boolean stabilize(
            final RegisterTypeRequest registerTypeRequest,
            final RegisterTypeResponse registerTypeResponse,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        final String registrationToken = callbackContext.getRegistrationToken();

        final DescribeTypeRegistrationResponse dtrResponse = describeModuleRegistration(
                Translator.translateToDescribeTypeRegistrationRequest(registrationToken), proxyClient, model);

        final String typeVersionArn = dtrResponse.typeVersionArn();
        if (typeVersionArn != null) {
            if (model.getArn() != null && !model.getArn().equals(typeVersionArn)) {
                logger.log(String.format("ARN for module version changed during stabilization, module=%s arn_before=%s arn_after=%s",
                        model.getModuleName(), model.getArn(), typeVersionArn));
            }
            model.setArn(typeVersionArn);
        } else {
            throw new CfnGeneralServiceException(String.format("ARN not provided during stabilization for module version, module=%s", model.getModuleName()));
        }

        switch (dtrResponse.progressStatus()) {
            case COMPLETE:
                logger.log(String.format("Module version registration for %s with registration token %s stabilized: %s\nModule version ARN: %s",
                        model.getModuleName(), registrationToken, dtrResponse.description(), model.getArn()));
                return true;
            case IN_PROGRESS:
                logger.log(String.format("Module version registration for %s with registration token %s in progress: %s",
                        model.getModuleName(), registrationToken, dtrResponse.description()));
                return false;
            case FAILED:
                logger.log(String.format("Module version registration for %s with registration token %s failed to stabilize: %s",
                        model.getModuleName(), registrationToken, dtrResponse.description()));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getArn());
            default:
                logger.log(String.format("Unexpected registration status for new version of module %s with registration token %s: %s",
                        model.getModuleName(), registrationToken, dtrResponse.description()));
                throw new CfnGeneralServiceException(String.format("received unexpected module registration status: %s", dtrResponse.progressStatus()));
        }
    }
}
