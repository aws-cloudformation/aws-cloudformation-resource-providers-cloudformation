package software.amazon.cloudformation.stackv2;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SdkClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [check if resource already exists]
            // if target API does not support 'ResourceAlreadyExistsException' then following check is required
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            .then(progress ->
                // STEP 1.0 [initialize a proxy context]
                // If your service API is not idempotent, meaning it does not distinguish duplicate create requests against some identifier (e.g; resource Name)
                // and instead returns a 200 even though a resource already exists, you must first check if the resource exists here
                // NOTE: If your service API throws 'ResourceAlreadyExistsException' for create requests this method is not necessary
                proxy.initiate("AWS-CloudFormation-StackV2::Create::PreExistanceCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 1.1 [TODO: construct a body of a request]
                    .translateToServiceRequest(Translator::translateToReadRequest)

                    // STEP 1.2 [TODO: make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        AwsResponse awsResponse = null;

                        // TODO: add custom read resource logic

                        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })

                    // STEP 1.3 [TODO: handle exception]
                    .handleError((awsRequest, exception, client, model, context) -> {
                        // TODO: uncomment when ready to implement
                        // if (exception instanceof CfnNotFoundException)
                        //     return ProgressEvent.progress(model, context);
                        // throw exception;
                        return ProgressEvent.progress(model, context);
                    })
                    .progress()
            )

            // STEP 2 [create/stabilize progress chain - required for resource creation]
            .then(progress ->
                // If your service API throws 'ResourceAlreadyExistsException' for create requests then CreateHandler can return just proxy.initiate construction
                // STEP 2.0 [initialize a proxy context]
                // Implement client invocation of the create request through the proxyClient, which is already initialised with
                // caller credentials, correct region and retry settings
                proxy.initiate("AWS-CloudFormation-StackV2::Create", proxyClient,progress.getResourceModel(), progress.getCallbackContext())

                    // STEP 2.1 [TODO: construct a body of a request]
                    .translateToServiceRequest(Translator::translateToCreateRequest)

                    // STEP 2.2 [TODO: make an api call]
                    .makeServiceCall((awsRequest, client) -> {
                        AwsResponse awsResponse = null;
                        try {

                            // TODO: put your create resource code here

                        } catch (final AwsServiceException e) {
                            /*
                            * While the handler contract states that the handler must always return a progress event,
                            * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
                            * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
                            * to more specific error codes
                            */
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })

                    // STEP 2.3 [TODO: stabilize step is not necessarily required but typically involves describing the resource until it is in a certain status, though it can take many forms]
                    // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                    // If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
                    // accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        // TODO: put your stabilization code here

                        final boolean stabilized = true;
                        logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
                        return stabilized;
                    })
                    .progress()
                )

            // STEP 3 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
