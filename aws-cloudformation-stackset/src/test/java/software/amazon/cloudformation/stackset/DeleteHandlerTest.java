package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_NO_INSTANCES_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_NO_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    CloudFormationClient sdkClient;
    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<CloudFormationClient> proxyClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudFormationClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        when(proxyClient.client().deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(proxyClient.client().deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(proxyClient.client()).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_NoInstances_SimpleSuccess() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_NO_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_NO_INSTANCES_FOR_READ);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_OneInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_ONE_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(proxyClient.client().deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(proxyClient.client()).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).deleteStackSet(any(DeleteStackSetRequest.class));
    }
}
