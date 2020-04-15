package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.Stabilizer;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackSetRequest;
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

        // Delete resource
        if (!context.isStabilizationStarted()) {
            deleteStackInstances(proxy, model, logger, client, context);

        } else if (stabilizer.isStabilized(model, context)){
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

    private void deleteStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final Logger logger,
            final CloudFormationClient client,
            final CallbackContext context) {

        try {
            final DeleteStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                    deleteStackInstancesRequest(model.getStackSetId(),
                            model.getOperationPreferences(), model.getDeploymentTargets(), model.getRegions()),
                    client::deleteStackInstances);

            logger.log(String.format("%s [%s] stack instances deletion initiated",
                    ResourceModel.TYPE_NAME, model.getStackSetId()));

            context.setOperationId(response.operationId());
            context.setStabilizationStarted(true);

        } catch (final StackSetNotFoundException e) {
            throw new CfnNotFoundException(e);

        } catch (final OperationInProgressException e) {
            context.incrementRetryCounter();
        }
    }
}
