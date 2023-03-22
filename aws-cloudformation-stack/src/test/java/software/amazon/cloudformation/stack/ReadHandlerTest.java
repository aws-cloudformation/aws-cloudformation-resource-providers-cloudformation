package software.amazon.cloudformation.stack;

import java.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyRequest;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
public class ReadHandlerTest extends AbstractTestBase {

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
        when(proxyClient.client().getTemplate(any(GetTemplateRequest.class)))
            .thenReturn(GetTemplateResponse.builder()
                .templateBody(TEMPLATE_BODY)
                .build());
        when(proxyClient.client().getStackPolicy(any(GetStackPolicyRequest.class)))
            .thenReturn(GetStackPolicyResponse.builder()
                .stackPolicyBody(STACK_POLICY_BODY)
                .build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_CREATE_COMPLETE))
                .build());

        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
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
        assertThat(response.getResourceModel()).isEqualToIgnoringNullFields(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess1() {
        // Mocks
        when(proxyClient.client().getTemplate(any(GetTemplateRequest.class)))
            .thenReturn(GetTemplateResponse.builder()
                .templateBody(TEMPLATE_BODY)
                .build());
        when(proxyClient.client().getStackPolicy(any(GetStackPolicyRequest.class)))
            .thenReturn(GetStackPolicyResponse.builder()
                .stackPolicyBody(STACK_POLICY_BODY)
                .build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_UPDATE_COMPLETE))
                .build());

        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
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
        assertThat(response.getResourceModel()).isEqualToIgnoringNullFields(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void stackindeleteComplete_HandlerThrowsCfnNotFoundException() {
        // Mocks
        when(proxyClient.client().getTemplate(any(GetTemplateRequest.class)))
            .thenReturn(GetTemplateResponse.builder()
                .templateBody(TEMPLATE_BODY)
                .build());
        when(proxyClient.client().getStackPolicy(any(GetStackPolicyRequest.class)))
            .thenReturn(GetStackPolicyResponse.builder()
                .stackPolicyBody(STACK_POLICY_BODY)
                .build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of(STACK_DELETE_COMPLETE))
                .build());

        final ReadHandler handler = new ReadHandler();

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
    public void noStackinresponse_HandlerThrowsCfnNotFoundException() {
        // Mocks
        when(proxyClient.client().getTemplate(any(GetTemplateRequest.class)))
            .thenReturn(GetTemplateResponse.builder()
                .templateBody(TEMPLATE_BODY)
                .build());
        when(proxyClient.client().getStackPolicy(any(GetStackPolicyRequest.class)))
            .thenReturn(GetStackPolicyResponse.builder()
                .stackPolicyBody(STACK_POLICY_BODY)
                .build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenReturn(DescribeStacksResponse.builder()
                .stacks(ImmutableList.of())
                .build());

        final ReadHandler handler = new ReadHandler();

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
    public void serviceExceptionThrown_HandlerThrowsCfnGeneralServiceException() {
        // Mocks
        when(proxyClient.client().getTemplate(any(GetTemplateRequest.class)))
            .thenReturn(GetTemplateResponse.builder()
                .templateBody(TEMPLATE_BODY)
                .build());
        when(proxyClient.client().getStackPolicy(any(GetStackPolicyRequest.class)))
            .thenReturn(GetStackPolicyResponse.builder()
                .stackPolicyBody(STACK_POLICY_BODY)
                .build());
        when(proxyClient.client().describeStacks(any(DescribeStacksRequest.class)))
            .thenThrow(AwsServiceException.builder().message("service error").build());

        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder()
            .stackId(STACK_ID)
            .stackName(STACK_NAME)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }
}
