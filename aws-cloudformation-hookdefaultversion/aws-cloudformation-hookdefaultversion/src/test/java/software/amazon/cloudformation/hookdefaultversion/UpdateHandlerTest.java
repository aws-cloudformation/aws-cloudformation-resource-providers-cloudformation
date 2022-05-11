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
public class UpdateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private static final String TYPE_NAME = "AWS::Demo::Resource";
    private static final String VERSION_ID_1 = "00000001";
    private static final String VERSION_ID_2 = "00000002";
    private static final String TYPE = "RESOURCE";
    private static final String TYPE_VERSION_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001";
    private static final String ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource";

    private final UpdateHandler handler = new UpdateHandler();

    protected UpdateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_TypeNameAndVersion_Success() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .versionId(VERSION_ID_2)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN)
                .defaultVersionId(VERSION_ID_1)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .versionId(VERSION_ID_1)
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
                .typeVersionArn(TYPE_VERSION_ARN)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN)
                .defaultVersionId(VERSION_ID_1)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN)
                .typeName(TYPE_NAME)
                .arn(ARN)
                .versionId(VERSION_ID_1)
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
    public void handleRequest_NotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN)
                .typeName(TYPE_NAME)
                .build();

        when(client.setTypeDefaultVersion(ArgumentMatchers.any(SetTypeDefaultVersionRequest.class)))
                .thenThrow(make(
                        TypeNotFoundException.builder(), 404, "Type not found",
                        TypeNotFoundException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
