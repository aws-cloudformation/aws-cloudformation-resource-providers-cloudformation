package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {
    private DeleteHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_Success() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .defaultVersionId("00000001")
            .deprecatedStatus("LIVE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .type("RESOURCE")
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .build();

        final DeregisterTypeResponse deregisterTypeResponse = DeregisterTypeResponse.builder().build();

        doReturn(describeTypeResponse, deregisterTypeResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

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
    public void handleRequest_NotFound() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier '{\"/properties/Arn\":\"arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001\"}' was not found.")
            .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void handleRequest_GeneralError() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .defaultVersionId("00000001")
            .deprecatedStatus("LIVE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .type("RESOURCE")
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .build();

        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(describeTypeResponse)
            .thenThrow(CfnRegistryException.builder().message("some error").build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Error occurred during operation 'DeregisterType: some error'.")
            .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_NotFoundAfterRead() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .defaultVersionId("00000001")
            .deprecatedStatus("LIVE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .type("RESOURCE")
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .build();

        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(describeTypeResponse)
            .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier '{\"/properties/Arn\":\"arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001\"}' was not found.")
            .isExactlyInstanceOf(ResourceNotFoundException.class);
    }
}
