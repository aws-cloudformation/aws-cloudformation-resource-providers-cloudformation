package software.amazon.cloudformation.hookdefaultversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private static final String ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String VERSION_ID = "00000001";
    private static final String TYPE = "HOOK";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";
    private static final String TYPE_NAME = "AWS::Demo::Hook";

    private final ReadHandler handler = new ReadHandler();

    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CloudFormationClient client = getServiceClient();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(ARN)
                .defaultVersionId(VERSION_ID)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceModel model = ResourceModel.builder()
                .typeVersionArn(ARN)
                .versionId(VERSION_ID)
                .arn(TYPE_ARN)
                .typeName(TYPE_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeVersionArn(ARN)
                .typeName(TYPE_NAME)
                .build();

        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
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
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
