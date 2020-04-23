package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.InsufficientCapabilitiesException;
import software.amazon.awssdk.services.cloudformation.model.LimitExceededException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.PhysicalIdGenerator;
import software.amazon.cloudformation.stackset.util.Stabilizer;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackSetRequest;
import static software.amazon.cloudformation.stackset.util.Comparator.isAddingStackInstances;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.ADD_INSTANCES;
import static software.amazon.cloudformation.stackset.util.Stabilizer.getDelaySeconds;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;
        final ResourceModel model = request.getDesiredResourceState();
        final CloudFormationClient client = ClientBuilder.getClient();
        final Stabilizer stabilizer = Stabilizer.builder().proxy(proxy).client(client).logger(logger).build();
        final OperationOperator operator = OperationOperator.builder()
                .client(client).desiredModel(model)
                .logger(logger).proxy(proxy).context(context)
                .build();
        InstancesAnalyzer.builder().desiredModel(model).build().analyzeForCreate(context);

        // Create a resource when a creation has not initialed
        if (!context.isStackSetCreated()) {
            new Validator().validateTemplate(proxy, model.getTemplateBody(), model.getTemplateURL(), logger);
            final String stackSetName = PhysicalIdGenerator.generatePhysicalId(request);
            createStackSet(proxy, model, logger, client, context, stackSetName, request.getClientRequestToken());
        }

        if (stabilizer.isPerformingOperation(isAddingStackInstances(context), context.isAddStacksStarted(), null,
                ADD_INSTANCES, context.getCreateStacksQueue(), model, context)) {

            operator.updateStackSet(ADD_INSTANCES);
        }

        if (context.getOperationsStabilizationMap().get(ADD_INSTANCES)) {
            return ProgressEvent.defaultSuccessHandler(model);
        }

        return ProgressEvent.defaultInProgressHandler(
                context,
                getDelaySeconds(context),
                model);
    }

    private void createStackSet(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final Logger logger,
            final CloudFormationClient client,
            final CallbackContext context,
            final String stackSetName,
            final String requestToken) {

        try {
            final CreateStackSetResponse response = proxy.injectCredentialsAndInvokeV2(
                    createStackSetRequest(model, stackSetName, requestToken), client::createStackSet);
            model.setStackSetId(response.stackSetId());

            logger.log(String.format("%s [%s] StackSet creation succeeded", ResourceModel.TYPE_NAME, stackSetName));
            context.setStackSetCreated(true);

        } catch (final AlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);

        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);

        } catch (final InsufficientCapabilitiesException e) {
            throw new CfnInvalidRequestException(e);
        }
    }
}
