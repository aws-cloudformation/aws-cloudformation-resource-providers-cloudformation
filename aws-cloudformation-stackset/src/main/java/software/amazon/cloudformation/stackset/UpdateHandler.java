package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.Stabilizer;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.util.Comparator.isAddingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isDeletingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;
import static software.amazon.cloudformation.stackset.util.Comparator.isUpdatingStackInstances;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.ADD_INSTANCES;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.DELETE_INSTANCES;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.STACK_SET_CONFIGS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.UPDATE_INSTANCES;
import static software.amazon.cloudformation.stackset.util.Stabilizer.getDelaySeconds;
import static software.amazon.cloudformation.stackset.util.Stabilizer.isUpdateStabilized;


public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;
        final CloudFormationClient client = ClientBuilder.getClient();
        final ResourceModel previousModel = request.getPreviousResourceState();
        final ResourceModel desiredModel = request.getDesiredResourceState();
        final Stabilizer stabilizer = Stabilizer.builder().proxy(proxy).client(client).logger(logger).build();
        final OperationOperator operator = OperationOperator.builder()
                .client(client).desiredModel(desiredModel).previousModel(previousModel)
                .logger(logger).proxy(proxy).context(context)
                .build();
        InstancesAnalyzer.builder().desiredModel(desiredModel).previousModel(previousModel).build()
                .analyzeForUpdate(context);

        final boolean isStackSetUpdating = !isStackSetConfigEquals(previousModel, desiredModel);
        if (stabilizer.isPerformingOperation(isStackSetUpdating, context.isUpdateStackSetStarted(),null,
                STACK_SET_CONFIGS, null, desiredModel, context)) {

            new Validator().validateTemplate(proxy, desiredModel.getTemplateBody(), desiredModel.getTemplateURL(), logger);
            operator.updateStackSet(STACK_SET_CONFIGS);
        }

        if (stabilizer.isPerformingOperation(isDeletingStackInstances(context), context.isDeleteStacksStarted(),
                STACK_SET_CONFIGS, DELETE_INSTANCES, context.getDeleteStacksQueue(), desiredModel, context)) {

            operator.updateStackSet(DELETE_INSTANCES);
        }

        if (stabilizer.isPerformingOperation(isAddingStackInstances(context), context.isAddStacksStarted(),
                DELETE_INSTANCES, ADD_INSTANCES, context.getCreateStacksQueue(), desiredModel, context)) {

            operator.updateStackSet(ADD_INSTANCES);
        }

        if (stabilizer.isPerformingOperation(isUpdatingStackInstances(context), context.isUpdateStacksStarted(),
                ADD_INSTANCES, UPDATE_INSTANCES, context.getUpdateStacksQueue(), desiredModel, context)) {

            operator.updateStackSet(UPDATE_INSTANCES);
        }

        if (isUpdateStabilized(context)) {
            return ProgressEvent.defaultSuccessHandler(desiredModel);

        } else {
            return ProgressEvent.defaultInProgressHandler(
                    context,
                    getDelaySeconds(context),
                    desiredModel);
        }
    }
}
