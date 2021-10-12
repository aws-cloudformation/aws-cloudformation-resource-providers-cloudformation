package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CallAs;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SERVICE_MANAGED_MODEL_FOR_READ;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_DELEGATED_ADMIN_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_DELETED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_NULL_PERMISSION_MODEL_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.EMPTY_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LIST_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.READ_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.READ_MODEL_DELEGATED_ADMIN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL_FOR_READ;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private ReadHandler handler;
    private CloudFormationClient client;
    private ResourceHandlerRequest<ResourceModel> request;
    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
    }

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        client = getServiceClient();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(READ_MODEL)
                .build();
    }

    @Test
    public void handleRequest_SelfManagedSS_Success() {

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_FOR_READ);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).listStackInstances(any(ListStackInstancesRequest.class));
    }

    @Test
    public void handleRequest_PermissionModelIsNull() {

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_NULL_PERMISSION_MODEL_STACK_SET_RESPONSE);
        when(client.listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SELF_MANAGED_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL_FOR_READ);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).listStackInstances(any(ListStackInstancesRequest.class));
    }

    @Test
    public void handleRequest_ContractTestNullId_FailedWithNotFoundException() {
        ResourceHandlerRequest<ResourceModel> emptyModelRequest = request.toBuilder()
                .desiredResourceState(EMPTY_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, emptyModelRequest, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("StackSets is not found");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_ServiceManaged_Member_ServiceError() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(READ_MODEL_DELEGATED_ADMIN)
                .build();
        AwsServiceException validationException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("ValidationError")
                        .build())
                .build();
        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenThrow(StackSetNotFoundException.class)
                .thenThrow(validationException);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy));

        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == null));
        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_ServiceManaged_DelegatedAdmin_Success() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(READ_MODEL_DELEGATED_ADMIN)
                .build();
        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenThrow(StackSetNotFoundException.class)
                .thenReturn(DESCRIBE_DELEGATED_ADMIN_SERVICE_MANAGED_STACK_SET_RESPONSE);
        when(client.listStackInstances(any(ListStackInstancesRequest.class)))
                .thenReturn(LIST_SERVICE_MANAGED_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL_FOR_READ);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == null));
        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).listStackInstances(argThat(
                (ListStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_ServiceManaged_ServiceError() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(READ_MODEL_DELEGATED_ADMIN)
                .build();
        AwsServiceException throttlingException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("ThrottlingException")
                        .build())
                .build();
        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenThrow(StackSetNotFoundException.class)
                .thenThrow(throttlingException);

        AwsServiceException thrown = assertThrows(AwsServiceException.class, () -> handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy));
        assertThat(thrown.awsErrorDetails().errorCode()).isEqualTo("ThrottlingException");

        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == null));
        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_DeletedStackSet_CfnNotFoundException() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(SELF_MANAGED_MODEL_FOR_READ)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_DELETED_STACK_SET_RESPONSE);

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy));
    }

    @Test
    public void handleRequest_NoStackSetsFound_CfnNotFoundException() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(SELF_MANAGED_MODEL_FOR_READ)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenThrow(StackSetNotFoundException.class);

        assertThrows(
                CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy));
    }

    @Test
    public void handleRequest_ServiceManaged_Member_AwsServiceException_NoDetails() {
        ResourceHandlerRequest<ResourceModel> serviceManagedRequest = request.toBuilder()
                .desiredResourceState(READ_MODEL_DELEGATED_ADMIN)
                .build();
        AwsServiceException validationException = AwsServiceException.builder()
                .build();
        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenThrow(StackSetNotFoundException.class)
                .thenThrow(validationException);

        assertThrows(AwsServiceException.class, () -> handler.handleRequest(proxy, serviceManagedRequest, null, loggerProxy));

        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == null));
        verify(client).describeStackSet(argThat(
                (DescribeStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }
}
