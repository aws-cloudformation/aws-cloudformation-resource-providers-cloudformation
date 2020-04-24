package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.InsufficientCapabilitiesException;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.Validator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.InvalidRequest;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_STOPPED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

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
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_ServiceManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxyClient.client())
                .createStackSet(any(CreateStackSetRequest.class));
        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxyClient.client())
                .createStackInstances(any(CreateStackInstancesRequest.class));
        doReturn(OPERATION_SUCCEED_RESPONSE).when(proxyClient.client())
                .describeStackSetOperation(any(DescribeStackSetOperationRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SelfManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxyClient.client())
                .createStackSet(any(CreateStackSetRequest.class));
        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxyClient.client())
                .createStackInstances(any(CreateStackInstancesRequest.class));
        doReturn(OPERATION_SUCCEED_RESPONSE).when(proxyClient.client())
                .describeStackSetOperation(any(DescribeStackSetOperationRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SelfManagedSS_NotStabilized() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxyClient.client())
                .createStackSet(any(CreateStackSetRequest.class));
        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxyClient.client())
                .createStackInstances(any(CreateStackInstancesRequest.class));
        doReturn(OPERATION_SUCCEED_RESPONSE).when(proxyClient.client())
                .describeStackSetOperation(any(DescribeStackSetOperationRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequest_InsufficientCapabilitiesException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doThrow(InsufficientCapabilitiesException.class).when(proxyClient.client())
                .createStackSet(any(CreateStackSetRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isNotNull();
    }

    @Test
    public void handlerRequest_OperationStoppedError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxyClient.client())
                .createStackSet(any(CreateStackSetRequest.class));
        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxyClient.client())
                .createStackInstances(any(CreateStackInstancesRequest.class));
        doReturn(OPERATION_STOPPED_RESPONSE).when(proxyClient.client())
                .describeStackSetOperation(any(DescribeStackSetOperationRequest.class));

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(InvalidRequest);
    }
}
