package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private DeleteHandler handler = new DeleteHandler();

    protected DeleteHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .defaultVersionId("00000001")
                .deprecatedStatus("LIVE")
                .isDefaultVersion(false)
                .sourceUrl("https://github.com/myorg/resource/repo.git")
                .type("RESOURCE")
                .typeName("AWS::Demo::Resource")
                .visibility("PRIVATE")
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final DeregisterTypeResponse deregisterTypeResponse = DeregisterTypeResponse.builder().build();
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenReturn(deregisterTypeResponse);

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .isDefaultVersion(false)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition("aws")
                .region("us-west-2")
                .awsAccountId("123456789012")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_GeneralError() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .typeName("AWS::Demo::Resource")
                .isDefaultVersion(true)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .defaultVersionId("00000001")
                .deprecatedStatus("LIVE")
                .isDefaultVersion(false)
                .sourceUrl("https://github.com/myorg/resource/repo.git")
                .type("RESOURCE")
                .typeName("AWS::Demo::Resource")
                .visibility("PRIVATE")
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        // throw on DeregisterType
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + resourceModel.getArn() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_CantDeleteDefaultVersion() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .typeName("AWS::Demo::Resource")
                .isDefaultVersion(true)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .defaultVersionId("00000001")
                .deprecatedStatus("LIVE")
                .isDefaultVersion(true)
                .sourceUrl("https://github.com/myorg/resource/repo.git")
                .type("RESOURCE")
                .typeName("AWS::Demo::Resource")
                .visibility("PRIVATE")
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + resourceModel.getArn() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }


    @Test
    public void handleRequest_GeneralServiceException() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .typeName("AWS::Demo::Resource")
                .isDefaultVersion(true)
                .build();

        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(NullPointerException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }
}
