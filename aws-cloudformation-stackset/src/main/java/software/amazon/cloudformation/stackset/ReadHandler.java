package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CallAs;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ResourceModelBuilder;

public class ReadHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // Contract test - MUST return FAILED with a NotFound error code
        if (StringUtils.isEmpty(model.getStackSetId())) {
            return ProgressEvent.failed(null, callbackContext, HandlerErrorCode.NotFound, "StackSets is not found");
        }

        StackSet stackSet;
        String callAs = null;

        /*
        * Evil, no-good hack to get around the issue that read handlers only pass in primary identifiers
        *
        * Because the primary identifier is a stack set ID, not a stack set name, it can only
        * exist either in the management account or the member account, not both. Calling
        * describe as SELF, then DA if the stack set is not found, will allow us to get the stack set,
        * so long as the calling account is a management account or a delegated administrator
        */
        try {
            stackSet = describeStackSet(proxyClient, model.getStackSetId(), logger);
        } catch (StackSetNotFoundException notFoundException) {
            logger.log(String.format("StackSet [%s] not found in SELF mode", model.getStackSetId()));
            callAs = CallAs.DELEGATED_ADMIN.name();
            try {
                stackSet = describeStackSet(proxyClient, model.getStackSetId(), callAs, logger);
            } catch (AwsServiceException serviceException) {

                // Map StackSetNotFoundException to CfnNotFoundException for contract test
                if (serviceException instanceof StackSetNotFoundException) {
                    logger.log(String.format("StackSet [%s] not found in DELEGATED_ADMIN mode", model.getStackSetId()));
                    throw new CfnNotFoundException(serviceException);
                }
                // A validation error here and not in the previous call should be the result from
                // the user not being a delegated administrator; map to CfnNotFoundException
                if (serviceException.awsErrorDetails() != null
                        && "ValidationError".equals(serviceException.awsErrorDetails().errorCode())) {
                    logger.log(String.format("Encountered ValidationError when finding StackSet [%s] in DELEGATED_ADMIN mode", model.getStackSetId()));
                    throw new CfnNotFoundException(notFoundException);
                }

                throw serviceException;
            }
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(ResourceModelBuilder.builder()
                        .proxyClient(proxyClient)
                        .stackSet(stackSet)
                        .build()
                        .buildModel(callAs))
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
