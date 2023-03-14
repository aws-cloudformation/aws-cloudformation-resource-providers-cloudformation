package software.amazon.cloudformation.stack;

import java.time.temporal.TemporalAmount;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import java.time.Instant;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackDriftInformation;
import software.amazon.awssdk.services.cloudformation.model.StackDriftStatus;
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

  protected static final Instant CREATION_TIME = Instant.now().minusMillis(5000);
  protected static final Instant LAST_UPDATED_TIME = Instant.now();

  protected static final  StackDriftInformation STACK_DRIFT_INFORMATION = software.amazon.awssdk.services.cloudformation.model.StackDriftInformation.builder().stackDriftStatus("NOT_CHECKED").build();
  protected static final Parameter PARAMETER = Parameter.builder().parameterKey("par").parameterValue("default").build();

  protected static final software.amazon.awssdk.services.cloudformation.model.Output OUTPUT = software.amazon.awssdk.services.cloudformation.model.Output.builder().outputKey("output").outputValue("outputValue").build();
  protected static final software.amazon.awssdk.services.cloudformation.model.Tag TAG = software.amazon.awssdk.services.cloudformation.model.Tag.builder().key("tag1").value("tagValue1").build();

  protected static final String STACK_POLICY_BODY =  "{\n  \"Statement\" : [\n    {\n      \"Effect\" : \"Allow\",\n      \"Action\" : \"Update:*\",\n      \"Principal\": \"*\",\n      \"Resource\" : \"*\"\n    }\n  ]\n}";

  protected static final Stack STACK_CREATE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.CREATE_COMPLETE)
      .creationTime(CREATION_TIME)
      .driftInformation(STACK_DRIFT_INFORMATION)
      .parameters(ImmutableList.of(PARAMETER))
      .outputs(ImmutableList.of(OUTPUT))
      .tags(ImmutableList.of(TAG))
      .build();
  protected static final Stack STACK_CREATE_FAILED = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.CREATE_FAILED)
      .creationTime(CREATION_TIME)
      .build();
  protected static final Stack STACK_CREATE_IN_PROGRESS = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.CREATE_IN_PROGRESS)
      .build();
  protected static final Stack STACK_UPDATE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .rootId("arn:aws:cloudformation:us-east-1:0111111111111:stack/test/7350d520-b85d-11ed-b590-0ec7beaead8f")
      .parentId("arn:aws:cloudformation:us-east-1:0111111111111:stack/test/7350d520-b85d-11ed-b590-0ec7beaead8f")
      .stackStatus(StackStatus.UPDATE_COMPLETE)
      .creationTime(CREATION_TIME)
      .lastUpdatedTime(LAST_UPDATED_TIME)
      .driftInformation(STACK_DRIFT_INFORMATION)
      .build();
  protected static final Stack STACK_UPDATE_IN_PROGRESS = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.UPDATE_IN_PROGRESS)
      .creationTime(CREATION_TIME)
      .lastUpdatedTime(LAST_UPDATED_TIME)
      .driftInformation(STACK_DRIFT_INFORMATION)
      .build();
  protected static final Stack STACK_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS)
      .creationTime(CREATION_TIME)
      .lastUpdatedTime(LAST_UPDATED_TIME)
      .driftInformation(STACK_DRIFT_INFORMATION)
      .build();
  protected static final Stack STACK_DELETE_IN_PROGRESS = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.DELETE_IN_PROGRESS)
      .build();
  protected static final Stack STACK_DELETE_FAILED = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.DELETE_FAILED)
      .build();
  protected static final Stack STACK_DELETE_COMPLETE = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus(StackStatus.DELETE_COMPLETE)
      .build();
  protected static final Stack STACK_UPDATE_FAILED = Stack.builder()
      .stackId(STACK_ID)
      .stackName(STACK_NAME)
      .stackStatus("UPDATE_FAILED")
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
