package software.amazon.cloudformation.stackset;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CallAs;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.InvalidRequest;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.DIFF;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstancesWithRegions;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateModel;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_DELEGATED_ADMIN_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_NO_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL_AS_SELF;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private ResourceHandlerRequest<ResourceModel> request;
    private DeleteHandler handler;
    private CloudFormationClient client;
    protected DeleteHandlerTest() {
        super(CloudFormationClient.class);
    }

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        client = getServiceClient();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_NoInstances_SimpleSuccess() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_NO_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_OneInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_ONE_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_AltModel_SimpleSuccess() {
        ResourceModel modelToDelete = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelToDelete)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client, times(3)).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client, times(3)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client).deleteStackSet(any(DeleteStackSetRequest.class));
    }

    @Test
    public void handleRequest_ServiceManagedSS_WithCallAsSelf_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL_AS_SELF)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackInstances(argThat(
                (DeleteStackInstancesRequest req) -> req.callAs() == CallAs.SELF));
        verify(client).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.SELF));
        verify(client).deleteStackSet(argThat(
                (DeleteStackSetRequest req) -> req.callAs() == CallAs.SELF));
    }

    @Test
    public void handleRequest_ServiceManagedSS_WithCallAs_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);
        when(client.deleteStackSet(any(DeleteStackSetRequest.class)))
                .thenReturn(DELETE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackInstances(argThat(
                (DeleteStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).deleteStackSet(argThat(
                (DeleteStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_SelfManagedSS_WithCallAsDelegatedAdmin_Failure() {

        AwsServiceException e = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("ValidationError")
                        .sdkHttpResponse(SdkHttpResponse.builder()
                                .statusCode(HttpStatusCode.BAD_REQUEST)
                                .build())
                        .build())
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(DELEGATED_ADMIN_SELF_MANAGED_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_DELEGATED_ADMIN_SERVICE_MANAGED_STACK_SET_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenThrow(e);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(InvalidRequest);

        verify(client).describeStackSet(any(DescribeStackSetRequest.class));
        verify(client).deleteStackInstances(argThat(
                (DeleteStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }
}
