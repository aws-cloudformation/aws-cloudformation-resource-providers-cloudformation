package software.amazon.cloudformation.stackv2;

import java.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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
public class CreateHandlerTest extends AbstractTestBase {

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
        // Mocks
        when(proxyClient.client().createStack(any(CreateStackRequest.class)))
            .thenReturn(CreateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenThrow(CloudFormationException.builder().message(NOT_FOUND_ERROR_MESSAGE).build())
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_IN_PROGRESS))
                .build())
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_COMPLETE))
                .build());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
            .templateURL(TEMPLATE_URL)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .systemTags(ImmutableMap.of("aws:key", "value"))
            .desiredResourceTags(ImmutableMap.of("key", "value"))
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToIgnoringGivenFields(request.getDesiredResourceState(), "templateURL", "tags");
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void stackNameIsNull_shouldReturnSuccessWithGeneratedStackName() {
        // Mocks
        when(proxyClient.client().createStack(any(CreateStackRequest.class)))
            .thenReturn(CreateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_COMPLETE))
                .build());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
            .templateURL(TEMPLATE_URL)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToIgnoringGivenFields(request.getDesiredResourceState(), "templateURL", "stackName");
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void serviceExceptionThrown_DescribeStackThrowsCfnGeneralServiceException() {
        // Mocks
        when(proxyClient.client().createStack(any(CreateStackRequest.class)))
            .thenReturn(CreateStackResponse.builder().stackId(STACK_ID).build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenThrow(CloudFormationException.builder().message("service error").build());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
            .templateURL(TEMPLATE_URL)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void serviceExceptionThrown_CreateStackThrowsCfnGeneralServiceException() {
        // Mocks
        when(proxyClient.client().createStack(any(CreateStackRequest.class)))
            .thenThrow(CloudFormationException.builder().message("service error").build());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
            .templateURL(TEMPLATE_URL)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger))
            .isInstanceOf(CfnGeneralServiceException.class);
    }
}
