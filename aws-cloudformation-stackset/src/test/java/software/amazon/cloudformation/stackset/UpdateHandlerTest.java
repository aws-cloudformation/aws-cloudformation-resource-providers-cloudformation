package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
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
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_4;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_SET_RESPONSE;

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
                .desiredResourceState(UPDATED_SELF_MANAGED_MODEL)
                .previousResourceState(SELF_MANAGED_MODEL)
                .build();

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
        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().describeStackInstance(any(DescribeStackInstanceRequest.class)))
                .thenReturn(DESCRIBE_STACK_INSTANCE_RESPONSE_1,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_2,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_3,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_4);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_FOR_READ);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).updateStackSet(any(UpdateStackSetRequest.class));
        verify(proxyClient.client()).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client()).updateStackInstances(any(UpdateStackInstancesRequest.class));
        verify(proxyClient.client()).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackInstance(any(DescribeStackInstanceRequest.class));
    }

    @Test
    public void handleRequest_NotUpdatable_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_MODEL)
                .previousResourceState(SIMPLE_MODEL)
                .build();

        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().describeStackInstance(any(DescribeStackInstanceRequest.class)))
                .thenReturn(DESCRIBE_STACK_INSTANCE_RESPONSE_1,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_2,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_3,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_4);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_FOR_READ);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackInstance(any(DescribeStackInstanceRequest.class));
    }
}
