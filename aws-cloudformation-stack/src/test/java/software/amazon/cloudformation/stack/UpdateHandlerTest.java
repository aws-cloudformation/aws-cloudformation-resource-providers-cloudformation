package software.amazon.cloudformation.stack;

import java.time.Duration;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateTerminationProtectionRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateTerminationProtectionResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<CloudFormationClient> proxyClient;

    @Mock
    CloudFormationClient sdkClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudFormationClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_COMPLETE))
                .build())
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_UPDATE_IN_PROGRESS))
                .build())
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_UPDATE_COMPLETE))
                .build());

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualToIgnoringNullFields(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void stabilize_no_stack_exist_throw_CfnNotFoundException() {
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of())
                .build());

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnNotFoundException.class);
    }
    @Test
    public void handlerRequest_Failure_handle_Error() {
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenThrow(AwsServiceException.builder().message("ServiceError").build());
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();


        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void no_update_should_return_success() {
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenThrow(CloudFormationException.builder().message("No updates are to be performed").build());
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualToIgnoringNullFields(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void stablization_Failure_handle_error() {
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenThrow(AwsServiceException.builder().message("Service Error").build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void stackReachesUpdateCompleteCleanup_HandlerReturnsSuccess() {
        // Mocks
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_COMPLETE))
                .build())
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS))
                .build());

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToIgnoringNullFields(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void describeStacksResponseIsEmpty_HandlerShouldThrowException() {
        // Mocks
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of())
                .build());

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void stackReachesUpdateFailed_HandlerThrowsNotStabilizedException() {
        // Mocks
        when(proxyClient.client().updateTerminationProtection(any(UpdateTerminationProtectionRequest.class)))
            .thenReturn(UpdateTerminationProtectionResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().updateStack(any(UpdateStackRequest.class)))
            .thenReturn(UpdateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_UPDATE_FAILED))
                .build());

        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnNotStabilizedException.class);
    }
}
