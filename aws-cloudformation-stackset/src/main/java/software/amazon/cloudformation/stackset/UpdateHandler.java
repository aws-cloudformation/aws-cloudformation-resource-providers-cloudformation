package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.Stabilizer;
import software.amazon.cloudformation.stackset.util.UpdatePlaceholder;
import software.amazon.cloudformation.stackset.util.Validator;

import java.util.Set;

import static software.amazon.cloudformation.stackset.util.Comparator.isAddingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isDeletingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;
import static software.amazon.cloudformation.stackset.util.Comparator.isUpdatingStackInstances;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_TARGETS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_TARGETS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.STACK_SET_CONFIGS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.getDelaySeconds;
import static software.amazon.cloudformation.stackset.util.Stabilizer.isPreviousOperationDone;
import static software.amazon.cloudformation.stackset.util.Stabilizer.isUpdateStabilized;


public class UpdateHandler extends BaseHandler<CallbackContext> {

    private Validator validator;

    public UpdateHandler() {
        this.validator = new Validator();
    }

    public UpdateHandler(Validator validator) {
        this.validator = validator;
    }

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

        final boolean isStackSetUpdating = !isStackSetConfigEquals(previousModel, desiredModel);
        final boolean isPerformingStackSetUpdate = stabilizer.isPerformingOperation(isStackSetUpdating,
                context.isUpdateStackSetStarted(), null, STACK_SET_CONFIGS, desiredModel, context);

        if (isPerformingStackSetUpdate) {
            if (previousModel.getTemplateURL() != desiredModel.getTemplateURL()) {
                validator.validateTemplate(
                        proxy, desiredModel.getTemplateBody(), desiredModel.getTemplateURL(), logger);
            }
            operator.updateStackSet(STACK_SET_CONFIGS,null, null);
        }

        final boolean isPerformingStackInstancesUpdate = isPreviousOperationDone(context, STACK_SET_CONFIGS) &&
                isUpdatingStackInstances(previousModel, desiredModel, context);

        if (isPerformingStackInstancesUpdate) {

            final UpdatePlaceholder updateTable = new UpdatePlaceholder(previousModel, desiredModel);
            final Set<String> regionsToAdd = updateTable.getRegionsToAdd();
            final Set<String> targetsToAdd = updateTable.getTargetsToAdd();
            final Set<String> regionsToDelete = updateTable.getRegionsToDelete();
            final Set<String> targetsToDelete = updateTable.getTargetsToDelete();

            if (isDeletingStackInstances(regionsToDelete, targetsToDelete, context)) {

                if (stabilizer.isPerformingOperation(
                        !regionsToDelete.isEmpty(), context.isDeleteStacksByRegionsStarted(),
                        STACK_SET_CONFIGS, DELETE_INSTANCES_BY_REGIONS, desiredModel, context)) {

                    operator.updateStackSet(DELETE_INSTANCES_BY_REGIONS, regionsToDelete, null);
                }

                if (stabilizer.isPerformingOperation(
                                !targetsToDelete.isEmpty(), context.isDeleteStacksByTargetsStarted(),
                                DELETE_INSTANCES_BY_REGIONS, DELETE_INSTANCES_BY_TARGETS, desiredModel, context)) {

                    operator.updateStackSet(DELETE_INSTANCES_BY_TARGETS, regionsToDelete, targetsToDelete);
                }
            }

            if (isAddingStackInstances(regionsToAdd, targetsToAdd, context)) {

                if (stabilizer.isPerformingOperation(
                                !regionsToAdd.isEmpty(), context.isAddStacksByRegionsStarted(),
                                DELETE_INSTANCES_BY_TARGETS, ADD_INSTANCES_BY_REGIONS, desiredModel, context)) {

                    operator.updateStackSet(ADD_INSTANCES_BY_REGIONS, regionsToAdd, null);
                }

                if (stabilizer.isPerformingOperation(
                                !targetsToAdd.isEmpty(), context.isAddStacksByTargetsStarted(),
                                ADD_INSTANCES_BY_REGIONS, ADD_INSTANCES_BY_TARGETS, desiredModel, context)) {

                    operator.updateStackSet(ADD_INSTANCES_BY_TARGETS, regionsToAdd, targetsToAdd);
                }
            }
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

