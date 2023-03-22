package software.amazon.cloudformation.stack;

import org.json.JSONObject;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Read Stack", request.getStackId(),
            request.getClientRequestToken()));
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> proxy.initiate("AWS-CloudFormation-Stack::GetTemplate", proxyClient, request.getDesiredResourceState(),
                callbackContext)
            .translateToServiceRequest(Translator::translateToGetTemplateRequest)
            .makeServiceCall((awsRequest, client) -> {
                GetTemplateResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::getTemplate);
                return awsResponse;
            })
            .handleError(
                (awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
            .done((req, res, cli, m, cc) -> {
                cc.setGetTemplateResponse(res);
                return ProgressEvent.progress(m, cc);
            }))
            .then(progress -> proxy.initiate("AWS-CloudFormation-Stack::GetStackPolicy", proxyClient, request.getDesiredResourceState(),
                    callbackContext)
                .translateToServiceRequest(Translator::translateToGetStackPolicyRequest)
                .makeServiceCall((awsRequest, client) -> {
                    GetStackPolicyResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::getStackPolicy);
                    return awsResponse;
                })
                .handleError(
                    (awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                .done((req, res, cli, m, cc) -> {
                    cc.setGetStackPolicyResponse(res);
                    return ProgressEvent.progress(m, cc);
                }))
            .then(progress -> proxy.initiate("AWS-CloudFormation-Stack::Read", proxyClient, request.getDesiredResourceState(),
                    callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                    DescribeStacksResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::describeStacks);
                    if (awsResponse.stacks().isEmpty() || awsResponse.stacks().get(0).stackStatus() == StackStatus.DELETE_COMPLETE) {
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getStackName());
                    }
                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    return awsResponse;
                })
                .handleError(
                    (awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                .done((req, res, cli, m, cc) -> {
                    cc.setDescribeStacksResponse(res);
                    return ProgressEvent.progress(m, cc);
                }))
            .then(progress -> {
                DescribeStacksResponse dr = callbackContext.getDescribeStacksResponse();
                GetStackPolicyResponse gr = callbackContext.getGetStackPolicyResponse();
                GetTemplateResponse tr = callbackContext.getGetTemplateResponse();
                ResourceModel finalModel =
                    dr.stacks().isEmpty() ? request.getDesiredResourceState() : Translator.translateFromReadResponse(dr.stacks().get(0));
                if (gr.stackPolicyBody() != null) finalModel.setStackPolicyBody(new JSONObject(gr.stackPolicyBody()).toMap());
                if (tr.templateBody() != null) finalModel.setTemplateBody(new JSONObject(tr.templateBody()).toMap());
                return ProgressEvent.defaultSuccessHandler(finalModel);
            });
    }

}
