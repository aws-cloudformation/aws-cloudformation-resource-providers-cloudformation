package software.amazon.cloudformation.stackset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.InsufficientCapabilitiesException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.PhysicalIdGenerator;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackSetRequest;

public class CreateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        final String stackSetName = PhysicalIdGenerator.generatePhysicalId(request);
        analyzeTemplate(proxy, model, callbackContext);

        return proxy.initiate("AWS-CloudFormation-StackSet::Create", proxyClient, model, callbackContext)
                .request(resourceModel ->
                        createStackSetRequest(resourceModel, stackSetName, request.getClientRequestToken()))
                .retry(MULTIPLE_OF)
                .call((modelRequest, proxyInvocation) -> createResource(modelRequest, proxyClient, model))
                .progress()
                .then(progress -> createStackInstances(proxy, proxyClient, progress, logger))
                .then(progress -> {
                    if (progress.isFailed()) return progress;
                    return ProgressEvent.defaultSuccessHandler(model);
                });
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateStackSetResponse createResource(
        final CreateStackSetRequest awsRequest,
        final ProxyClient<CloudFormationClient> proxyClient,
        final ResourceModel model) {

        CreateStackSetResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createStackSet);
            model.setStackSetId(response.stackSetId());

        } catch (final InsufficientCapabilitiesException e) {
            throw new CfnInvalidRequestException(e);
        }

        logger.log(String.format("%s [%s] StackSet creation succeeded", ResourceModel.TYPE_NAME, model.getStackSetId()));
        return response;
    }

    /**
     * Analyzes/validates template and StackInstancesGroup
     * @param proxy {@link AmazonWebServicesClientProxy}
     * @param model {@link ResourceModel}
     * @param context {@link CallbackContext}
     */
    private void analyzeTemplate(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final CallbackContext context) {

        new Validator().validateTemplate(proxy, model.getTemplateBody(), model.getTemplateURL(), logger);
        InstancesAnalyzer.builder().desiredModel(model).build().analyzeForCreate(context);
    }
}
