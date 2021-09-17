package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  public final int STACK_NAME_MAX_LENGTH = 16; // The service limit is actually 128

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
    return handleRequest(
      proxy,
      request,
      callbackContext != null ? callbackContext : new CallbackContext(),
      proxy.newProxy(ClientBuilder::getClient),
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<CloudFormationClient> proxyClient,
    final Logger logger);

  protected ProgressEvent<ResourceModel, CallbackContext> existenceCheck(
      ProgressEvent<ResourceModel, CallbackContext> progress,
      final AmazonWebServicesClientProxy proxy,
      final ProxyClient<CloudFormationClient> proxyClient,
      final CallbackContext context,
      final ResourceHandlerRequest<ResourceModel> request, String callGraphPrefix, Logger logger) {
    return progress
        .then(_progress ->
            proxy.initiate("AWS-CloudFormation-StackV2::Delete::PreDeletionCheck", proxyClient, _progress.getResourceModel(), _progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                  if (request.getDesiredResourceState().getArn() == null && request.getDesiredResourceState().getStackName() == null) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getArn());
                  }
                  DescribeStacksResponse awsResponse = null;
                  try {
                    awsResponse = client.injectCredentialsAndInvokeV2(DescribeStacksRequest.builder()
                        .stackName(_progress.getResourceModel().getArn()).build(), proxyClient.client()::describeStacks);
                  } catch (final AwsServiceException e) {
                    if (e.getMessage().contains("does not exist")) {
                      throw new CfnNotFoundException(e);
                    }
                    throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                  }
                  if (awsResponse.stacks().isEmpty() || awsResponse.stacks().get(0).stackStatus() == StackStatus.DELETE_COMPLETE) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getArn());
                  }
                  logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                  return awsResponse;
                })
                .progress());
  }
}
