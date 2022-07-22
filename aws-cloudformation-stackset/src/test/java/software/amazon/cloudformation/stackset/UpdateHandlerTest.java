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
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.AltStackInstancesCalculator;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.cloudformation.proxy.HandlerErrorCode.InvalidRequest;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.DIFF;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.INTER;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstancesWithRegions;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateModel;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.PREVIOUS_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_NO_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_WITH_ME_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_WITH_ME_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.VALID_TEMPLATE_SUMMARY_RESPONSE;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private UpdateHandler handler;
    private CloudFormationClient client;
    private ResourceHandlerRequest<ResourceModel> request;
    protected UpdateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @BeforeEach
    public void setup() {
        client = getServiceClient();
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_AltModel_SimpleSuccess() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF,
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_2, account_3), INTER,
                        new HashSet<>(Arrays.asList(region_2, region_3)))
        )));

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(currentModel)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.updateStackSet(any(UpdateStackSetRequest.class)))
                .thenReturn(UPDATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.updateStackInstances(any(UpdateStackInstancesRequest.class)))
                .thenReturn(UPDATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(currentModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).updateStackSet(any(UpdateStackSetRequest.class));
        verify(client, times(2)).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).updateStackInstances(any(UpdateStackInstancesRequest.class));
        verify(client, times(2)).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client, times(6)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SELF_MANAGED_MODEL)
                .desiredResourceState(UPDATED_SELF_MANAGED_MODEL)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.updateStackSet(any(UpdateStackSetRequest.class)))
                .thenReturn(UPDATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.updateStackInstances(any(UpdateStackInstancesRequest.class)))
                .thenReturn(UPDATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(UPDATED_SELF_MANAGED_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).updateStackSet(any(UpdateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).updateStackInstances(any(UpdateStackInstancesRequest.class));
        verify(client).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client, times(4)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handleRequest_ServiceManagedSS_WithCallAs_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SERVICE_MANAGED_MODEL)
                .desiredResourceState(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.updateStackSet(any(UpdateStackSetRequest.class)))
                .thenReturn(UPDATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.updateStackInstances(any(UpdateStackInstancesRequest.class)))
                .thenReturn(UPDATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).deleteStackInstances(argThat(
                (DeleteStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).updateStackSet(argThat(
                (UpdateStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).createStackInstances(argThat(
                (CreateStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).updateStackInstances(argThat(
                (UpdateStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client, times(4)).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
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
                .previousResourceState(SELF_MANAGED_MODEL)
                .desiredResourceState(DELEGATED_ADMIN_SELF_MANAGED_MODEL)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();


        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.updateStackSet(any(UpdateStackSetRequest.class)))
                .thenThrow(e);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(InvalidRequest);

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).updateStackSet(argThat(
                (UpdateStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_NotUpdatable_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SIMPLE_MODEL)
                .desiredResourceState(SIMPLE_MODEL)
                .previousResourceTags(DESIRED_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SIMPLE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
    }

    @Test
    public void handleRequest_UpdateManagedExecution_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(SELF_MANAGED_WITH_ME_MODEL)
                .desiredResourceState(UPDATED_SELF_MANAGED_WITH_ME_MODEL)
                .previousResourceTags(PREVIOUS_RESOURCE_TAGS)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .build();

        when(client.describeStackSet(any(DescribeStackSetRequest.class)))
                .thenReturn(DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE);
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.updateStackSet(any(UpdateStackSetRequest.class)))
                .thenReturn(UPDATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.deleteStackInstances(any(DeleteStackInstancesRequest.class)))
                .thenReturn(DELETE_STACK_INSTANCES_RESPONSE);
        when(client.updateStackInstances(any(UpdateStackInstancesRequest.class)))
                .thenReturn(UPDATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(UPDATED_SELF_MANAGED_WITH_ME_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client, times(2)).updateStackSet(any(UpdateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).updateStackInstances(any(UpdateStackInstancesRequest.class));
        verify(client).deleteStackInstances(any(DeleteStackInstancesRequest.class));
        verify(client, times(5)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }
}
