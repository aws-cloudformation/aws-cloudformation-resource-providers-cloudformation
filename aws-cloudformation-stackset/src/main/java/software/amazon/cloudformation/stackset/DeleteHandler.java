package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.Stabilizer;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackSetRequest;
import static software.amazon.cloudformation.stackset.util.Comparator.isDeletingStackInstances;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.DELETE_INSTANCES;
import static software.amazon.cloudformation.stackset.util.Stabilizer.getDelaySeconds;

public class DeleteHandler extends BaseHandler<CallbackContext> {

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
        InstancesAnalyzer.builder().desiredModel(model).build().analyzeForDelete(context);

        if (stabilizer.isPerformingOperation(isDeletingStackInstances(context), context.isDeleteStacksStarted(), null,
                DELETE_INSTANCES, context.getDeleteStacksQueue(), model, context)) {

            operator.updateStackSet(DELETE_INSTANCES);
        }

        if (context.getOperationsStabilizationMap().get(DELETE_INSTANCES)){
            deleteStackSet(proxy, model.getStackSetId(), logger, client);

            return ProgressEvent.defaultSuccessHandler(model);
        }

        return ProgressEvent.defaultInProgressHandler(
                context,
                getDelaySeconds(context),
                model);
    }

    private void deleteStackSet(
            final AmazonWebServicesClientProxy proxy,
            final String stackSetName,
            final Logger logger,
            final CloudFormationClient client) {

        try {
            proxy.injectCredentialsAndInvokeV2(deleteStackSetRequest(stackSetName), client::deleteStackSet);
            logger.log(String.format("%s [%s] StackSet deletion succeeded", ResourceModel.TYPE_NAME, stackSetName));

        } catch (final StackSetNotFoundException e) {
            throw new CfnNotFoundException(e);
        }
    }
}
