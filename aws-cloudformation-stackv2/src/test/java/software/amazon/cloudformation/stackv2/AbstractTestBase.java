package software.amazon.cloudformation.stackv2;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;
  protected static final String TEMPLATE_URL = "https://bucket.s3.amazonaws.com/template.yaml";
  protected static final String STACK_ID = "arn:aws:cloudformation:us-east-1:123412341234:stack/stack-name/" + UUID.randomUUID();
  protected static final String STACK_NAME = "stack-name";
  protected static final Stack STACK_CREATE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.CREATE_COMPLETE)
      .build();
  protected static final Stack STACK_DELETE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.DELETE_COMPLETE)
      .build();
  protected static final Stack STACK_UPDATE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.UPDATE_COMPLETE)
      .build();
  protected static final String NOT_FOUND_ERROR_MESSAGE = String.format("Stack with id %s does not exist (Service: CloudFormation, Status Code: 400, Request ID: %s, Extended Request ID: null)", STACK_ID, UUID.randomUUID());

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<CloudFormationClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final CloudFormationClient sdkClient) {
    return new ProxyClient<CloudFormationClient>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public CloudFormationClient client() {
        return sdkClient;
      }
    };
  }
}
