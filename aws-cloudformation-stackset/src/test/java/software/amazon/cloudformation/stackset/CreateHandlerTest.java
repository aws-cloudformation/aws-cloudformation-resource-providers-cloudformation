package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.InternalFailure;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_STACK_INSTANCE_RESPONSE_4;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_EMPTY_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_ONE_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_STOPPED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_DUPLICATE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_INVALID_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_NO_INSTANCES_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_ONE_INSTANCES_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_NO_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL_FOR_READ;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    CloudFormationClient sdkClient;
    private CreateHandler handler;
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
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_ServiceManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(proxyClient.client().createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SERVICE_MANAGED_STACK_SET_RESPONSE);
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
        assertThat(response.getResourceModel()).isEqualTo(SERVICE_MANAGED_MODEL_FOR_READ);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createStackSet(any(CreateStackSetRequest.class));
        verify(proxyClient.client()).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client()).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackInstance(any(DescribeStackInstanceRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(proxyClient.client().createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
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

        verify(proxyClient.client()).createStackSet(any(CreateStackSetRequest.class));
        verify(proxyClient.client(), times(2)).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client(), times(2)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
        verify(proxyClient.client(), times(4)).describeStackInstance(any(DescribeStackInstanceRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_NoInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_NO_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_EMPTY_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_NO_INSTANCES_FOR_READ);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createStackSet(any(CreateStackSetRequest.class));
        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_OneInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_ONE_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(proxyClient.client().createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(proxyClient.client().describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(proxyClient.client().listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_ONE_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackInstance(any(DescribeStackInstanceRequest.class)))
                .thenReturn(DESCRIBE_STACK_INSTANCE_RESPONSE_3,
                        DESCRIBE_STACK_INSTANCE_RESPONSE_4);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_ONE_INSTANCES_FOR_READ);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createStackSet(any(CreateStackSetRequest.class));
        verify(proxyClient.client()).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client()).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(proxyClient.client()).describeStackSet(any(DescribeStackSetRequest.class));
        verify(proxyClient.client()).listStackInstances(any(ListStackInstancesRequest.class));
        verify(proxyClient.client(), times(2)).describeStackInstance(any(DescribeStackInstanceRequest.class));
    }

    @Test
    public void handlerRequest_OperationStoppedError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(proxyClient.client().createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(proxyClient.client().createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(proxyClient.client().describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_STOPPED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(InternalFailure);

        verify(proxyClient.client()).createStackSet(any(CreateStackSetRequest.class));
        verify(proxyClient.client()).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(proxyClient.client()).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handlerRequest_CfnInvalidRequestException_DuplicateStackInstance() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_DUPLICATE_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handlerRequest_CfnInvalidRequestException_InvalidDeploymentTargets() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_INVALID_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }
}
