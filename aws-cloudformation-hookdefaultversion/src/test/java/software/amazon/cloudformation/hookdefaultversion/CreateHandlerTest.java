package software.amazon.cloudformation.hookdefaultversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String VERSION_ID = "00000002";
    private static final String ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";
    public static final String TYPE_VERSION_ARN_1 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String TYPE_VERSION_ARN_2 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000002";
    private static final String TYPE = "HOOK";
    private static final String AWS_PARTITION = "aws";
    private static final String REGION = "us-west-2";
    private static final String AWS_ACCOUNT_ID = "123456789012";


    private final CreateHandler handler = new CreateHandler();

    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_TypeNameAndVersion_Success() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .versionId(VERSION_ID)
                .arn(ARN)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponsePre = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(false)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        final DescribeTypeResponse describeTypeResponsePost = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(true)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponsePre)
                .thenReturn(describeTypeResponsePost);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();
        CallbackContext context = new CallbackContext();
        context.setArn(ARN);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, loggerProxy);

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_TypeArn_Success() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .arn(ARN)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponsePre = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(false)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        final DescribeTypeResponse describeTypeResponsePost = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(true)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponsePre)
                .thenReturn(describeTypeResponsePost);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .arn(ARN)
                .typeName(TYPE_NAME)
                .build();
        CallbackContext context = new CallbackContext();
        context.setArn(ARN);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .build();


        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenThrow(make(
                        TypeNotFoundException.builder(), 404, "Type not found",
                        TypeNotFoundException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        CallbackContext context = new CallbackContext();
        context.setArn(ARN);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_AsyncNotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .build();


        // verifies behaviour if type was deregistered out of band (timing conflict)
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_1)
                .isDefaultVersion(false)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenThrow(make(
                        TypeNotFoundException.builder(), 404, "Type not found",
                        TypeNotFoundException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        CallbackContext context = new CallbackContext();
        context.setArn(ARN);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_generateTypeArn_withArn() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponsePre = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(false)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        final DescribeTypeResponse describeTypeResponsePost = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(true)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponsePre)
                .thenReturn(describeTypeResponsePost);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN_2)
                .arn(ARN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_generateTypeArn_withVersionId() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .versionId(VERSION_ID)
                .typeName(TYPE_NAME)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponsePre = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(false)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        final DescribeTypeResponse describeTypeResponsePost = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_2)
                .isDefaultVersion(true)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponsePre)
                .thenReturn(describeTypeResponsePost);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .versionId(VERSION_ID)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
