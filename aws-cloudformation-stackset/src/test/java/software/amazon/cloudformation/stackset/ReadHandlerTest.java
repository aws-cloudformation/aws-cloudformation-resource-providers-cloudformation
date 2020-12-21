package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
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
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_4;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.READ_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_FOR_READ;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    CloudFormationClient sdkClient;
    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<CloudFormationClient> proxyClient;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(CloudFormationClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(READ_MODEL)
                .build();
    }

    @Test
    public void handleRequest_SelfManagedSS_Success() {

        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_RESPONSE);

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
    }
}
