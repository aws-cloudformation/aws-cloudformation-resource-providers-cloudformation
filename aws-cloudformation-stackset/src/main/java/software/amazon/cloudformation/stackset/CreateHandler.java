package software.amazon.cloudformation.stackset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.InsufficientCapabilitiesException;
import software.amazon.awssdk.services.cloudformation.model.LimitExceededException;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.PhysicalIdGenerator;
import software.amazon.cloudformation.stackset.util.Stabilizer;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackSetRequest;
import static software.amazon.cloudformation.stackset.util.Stabilizer.getDelaySeconds;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceModel model;
    private CloudFormationClient client;
    private CallbackContext context;
    private Logger logger;
    private Stabilizer stabilizer;

    @Builder.Default
    private Validator validator = new Validator();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;
        this.model = request.getDesiredResourceState();
        this.logger = logger;
        this.proxy = proxy;
        this.client = ClientBuilder.getClient();
        this.stabilizer = Stabilizer.builder().proxy(proxy).client(client).logger(logger).build();

        // Create a resource when a creation has not initialed
        if (!context.isStabilizationStarted()) {
            validator.validateTemplate(proxy, model.getTemplateBody(), model.getTemplateURL(), logger);
            final String stackSetName = PhysicalIdGenerator.generatePhysicalId(request);
            createStackSet(stackSetName, request.getClientRequestToken());

        } else if (stabilizer.isStabilized(model, context)) {
            return ProgressEvent.defaultSuccessHandler(model);
        }

        return ProgressEvent.defaultInProgressHandler(
                context,
                getDelaySeconds(context),
                model);
    }

    private void createStackSet(final String stackSetName, final String requestToken) {
        try {
            final CreateStackSetResponse response = proxy.injectCredentialsAndInvokeV2(
                    createStackSetRequest(model, stackSetName, requestToken), client::createStackSet);
            model.setStackSetId(response.stackSetId());

            logger.log(String.format("%s [%s] StackSet creation succeeded", ResourceModel.TYPE_NAME, stackSetName));

            createStackInstances(stackSetName);

        } catch (final AlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);

        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);

        } catch (final InsufficientCapabilitiesException e) {
            throw new CfnInvalidRequestException(e);
        }
    }

    private void createStackInstances(final String stackSetName) {
        try {
            final CreateStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                    createStackInstancesRequest(stackSetName, model.getOperationPreferences(),
                            model.getDeploymentTargets(), model.getRegions()),
                    client::createStackInstances);

            logger.log(String.format("%s [%s] stack instances creation initiated",
                    ResourceModel.TYPE_NAME, stackSetName));

            context.setStabilizationStarted(true);
            context.setOperationId(response.operationId());

        } catch (final StackSetNotFoundException e) {
            throw new CfnNotFoundException(e);

        } catch (final OperationInProgressException e) {
            context.incrementRetryCounter();
        }
    }
}
