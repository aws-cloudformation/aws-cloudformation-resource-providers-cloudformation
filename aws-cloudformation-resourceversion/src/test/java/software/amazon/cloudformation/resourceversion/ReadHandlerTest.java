package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    ReadHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .defaultVersionId("00000001")
            .deprecatedStatus("LIVE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .type("RESOURCE")
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .build();

        doReturn(describeTypeResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel inModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        final ResourceModel outModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .isDefaultVersion(true)
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .typeName("AWS::Demo::Resource")
            .versionId("00000001")
            .visibility("PRIVATE")
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(inModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(outModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeprecatedTypeDoesNotExist() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .defaultVersionId("00000001")
            .deprecatedStatus("DEPRECATED")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .type("RESOURCE")
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .build();

        doReturn(describeTypeResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThrows(ResourceNotFoundException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_GeneralError() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenThrow(CfnRegistryException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasCauseExactlyInstanceOf(CfnRegistryException.class)
            .hasMessage(null)
            .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
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
    public void handleRequest_BadInput_NoModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier 'null' was not found.")
            .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void handleRequest_BadInput_EmptyModel() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier 'null' was not found.")
            .isExactlyInstanceOf(ResourceNotFoundException.class);
    }
}
