package software.amazon.cloudformation.stack;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
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
import software.amazon.cloudformation.proxy.delay.Exponential;

import java.time.Duration;
import java.util.Optional;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    public final int STACK_NAME_MAX_LENGTH = 128;
    protected static final Exponential BACKOFF_STRATEGY = Exponential.of().timeout(Duration.ofHours(36L)).build();

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
            proxy.initiate(callGraphPrefix, proxyClient, _progress.getResourceModel(), _progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                  if (request.getDesiredResourceState().getStackId() == null && request.getDesiredResourceState().getStackName() == null) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getStackId());
                  }
                  DescribeStacksResponse awsResponse = null;
                  try {
                    logger.log(String.format("%s %s", this.getClass(), Optional.ofNullable(request.getClientRequestToken()).orElse("")));
                    awsResponse = client.injectCredentialsAndInvokeV2(DescribeStacksRequest.builder()
                        .stackName(_progress.getResourceModel().getStackId()).build(), proxyClient.client()::describeStacks);
                  } catch (final AwsServiceException e) {
                    if (e.getMessage().contains("does not exist")) {
                      throw new CfnNotFoundException(e);
                    }
                    throw new CfnGeneralServiceException(e.getMessage(), e);
                  }
                  if (awsResponse.stacks().isEmpty() || awsResponse.stacks().get(0).stackStatus() == StackStatus.DELETE_COMPLETE) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getStackId());
                  }
                  logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                  return awsResponse;
                })
                .progress());
  }
}
