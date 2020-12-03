package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.PREVIOUS_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.VALID_TEMPLATE_SUMMARY_RESPONSE;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    CloudFormationClient sdkClient;
    private UpdateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<CloudFormationClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudFormationClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_SelfManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SELF_MANAGED_MODEL)
                .desiredResourceState(UPDATED_SELF_MANAGED_MODEL)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(proxyClient.client().getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(proxyClient.client().updateStackSet(any(UpdateStackSetRequest.class)))
                .thenReturn(UPDATE_STACK_SET_RESPONSE);
        when(proxyClient.client().createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().updateStackInstances(any(UpdateStackInstancesRequest.class)))
                .thenReturn(UPDATE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(UPDATED_SELF_MANAGED_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(proxyClient.client()).updateStackSet(any(UpdateStackSetRequest.class));
        verify(proxyClient.client()).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client()).updateStackInstances(any(UpdateStackInstancesRequest.class));
        verify(proxyClient.client()).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handleRequest_NotUpdatable_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SIMPLE_MODEL)
                .desiredResourceState(SIMPLE_MODEL)
                .previousResourceTags(DESIRED_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(proxyClient.client().getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SIMPLE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTemplateSummary(any(GetTemplateSummaryRequest.class));
    }
}
