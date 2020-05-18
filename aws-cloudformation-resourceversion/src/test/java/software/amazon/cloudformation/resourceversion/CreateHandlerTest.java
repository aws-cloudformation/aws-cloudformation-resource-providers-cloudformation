package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {
    private CreateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
    }

//    @Test
    public void handleRequest_Success() {
        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder().build();
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.COMPLETE)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(registerTypeResponse)
            .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

//    @Test
    public void handleRequest_CreateFailed() {
        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenThrow(CfnRegistryException.builder().message("some exception").build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder().build())
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasCauseExactlyInstanceOf(CfnRegistryException.class)
            .hasMessage(null)
            .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

//    @Test
    public void handleRequest_StabilizeSuccess() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.COMPLETE)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final CallbackContext callbackContext = CallbackContext.builder().createStarted(true).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

//    @Test
    public void handleRequest_StabilizeNotFound() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenThrow(new CfnNotFoundException("Not Found", "Some Arn"));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final CallbackContext callbackContext = CallbackContext.builder().createStarted(true).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isEqualTo("Resource of type 'Not Found' with identifier 'Some Arn' was not found.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotStabilized);
    }

//    @Test
    public void handleRequest_StabilizeGeneralError() {
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenThrow(CfnRegistryException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final CallbackContext callbackContext = CallbackContext.builder().createStarted(true).build();
        assertThatThrownBy(() -> handler.handleRequest(proxy, request, callbackContext, logger))
            .hasCauseExactlyInstanceOf(CfnRegistryException.class)
            .hasMessage(null)
            .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

//    @Test
    public void handleRequest_StabilizeFailed() {
        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder().build();
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.FAILED)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(registerTypeResponse)
            .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier 'null' did not stabilize.")
            .isExactlyInstanceOf(CfnNotStabilizedException.class);
    }

//    @Test
    public void handleRequest_DelayedCompletion() {
        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder().build();
        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.IN_PROGRESS)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        when(proxy.injectCredentialsAndInvokeV2(
            ArgumentMatchers.any(),
            ArgumentMatchers.any())
        )
            .thenReturn(registerTypeResponse)
            .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CallbackContext.builder().createStarted(true).build());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(3);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
