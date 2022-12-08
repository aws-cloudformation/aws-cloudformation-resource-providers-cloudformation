package software.amazon.cloudformation.stackset;

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
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetOperationResultsRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.Arrays;
import java.util.HashSet;

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
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstancesWithRegions;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateModel;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_3;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELEGATED_ADMIN_SERVICE_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_DUPLICATE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_INVALID_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_NO_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_ONE_INSTANCES_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL_AS_SELF;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_SUMMARY_RESPONSE_WITH_NESTED_STACK;
import static software.amazon.cloudformation.stackset.util.TestUtils.VALID_TEMPLATE_SUMMARY_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.getFailedDescribeStackSetOperationResponse;
import static software.amazon.cloudformation.stackset.util.TestUtils.getListStackSetOperationResultsResponse;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private ResourceHandlerRequest<ResourceModel> request;
    private CreateHandler handler;
    private CloudFormationClient client;
    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        client = getServiceClient();
    }

    @Test
    public void handleRequest_ServiceManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL_AS_SELF)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SERVICE_MANAGED_MODEL_AS_SELF);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(argThat(
                (CreateStackSetRequest req) -> req.callAs() == CallAs.SELF));
        verify(client).createStackInstances(argThat(
                (CreateStackInstancesRequest req) -> req.callAs() == CallAs.SELF));
        verify(client).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.SELF));
    }

    @Test
    public void handleRequest_ServiceManagedSS_WithCallAsSelf_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
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
        verify(client).createStackSet(argThat(
                (CreateStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).createStackInstances(argThat(
                (CreateStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handleRequest_ServiceManagedSS_WithCallAsDelegatedAdmin_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(DELEGATED_ADMIN_SERVICE_MANAGED_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
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
        verify(client).createStackSet(argThat(
                (CreateStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).createStackInstances(argThat(
                (CreateStackInstancesRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
        verify(client).describeStackSetOperation(argThat(
                (DescribeStackSetOperationRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }


    @Test
    public void handleRequest_SelfManagedSS_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client, times(2)).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client, times(2)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handleRequest_AltModel_SimpleSuccess() {
        ResourceModel modelToCreate = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelToCreate)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(modelToCreate);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client, times(3)).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client, times(3)).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
    }

    @Test
    public void handleRequest_SelfManagedSS_NoInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_NO_INSTANCES_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_NO_INSTANCES_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));

    }

    @Test
    public void handleRequest_SelfManagedSS_OneInstances_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_ONE_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(OPERATION_SUCCEED_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(SELF_MANAGED_ONE_INSTANCES_MODEL);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
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

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenThrow(e);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(InvalidRequest);

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(argThat(
                (CreateStackSetRequest req) -> req.callAs() == CallAs.DELEGATED_ADMIN));
    }

    @Test
    public void handlerRequest_OperationStoppedError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
        String failedStatusReason = "failed status reason";

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
                .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
                .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
                .thenReturn(getFailedDescribeStackSetOperationResponse(failedStatusReason));

        Exception e = assertThrows(
                CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client, never()).listStackSetOperationResults(any(ListStackSetOperationResultsRequest.class));
        assertThat(e.getMessage()).contains(failedStatusReason);
    }

    @Test
    public void handlerRequest_OperationStoppedErrorCallsListStackSetOperationResults() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(SELF_MANAGED_MODEL)
            .desiredResourceTags(DESIRED_RESOURCE_TAGS)
            .logicalResourceIdentifier(LOGICAL_ID)
            .clientRequestToken(REQUEST_TOKEN)
            .build();
        String failedStatusReason = "another failed status reason";

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
            .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
            .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
            .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
            .thenReturn(getFailedDescribeStackSetOperationResponse(null));
        when(client.listStackSetOperationResults(any(ListStackSetOperationResultsRequest.class)))
            .thenReturn(getListStackSetOperationResultsResponse(failedStatusReason));

        Exception e = assertThrows(
            CfnNotStabilizedException.class,
            () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client).listStackSetOperationResults(any(ListStackSetOperationResultsRequest.class));
        assertThat(e.getMessage()).contains(failedStatusReason);
    }

    @Test
    public void handlerRequest_OperationStoppedErrorExceedsCfnCharacterLimit() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(SELF_MANAGED_MODEL)
            .desiredResourceTags(DESIRED_RESOURCE_TAGS)
            .logicalResourceIdentifier(LOGICAL_ID)
            .clientRequestToken(REQUEST_TOKEN)
            .build();
        String failedStatusReason = new String(new char[30]).replace("\0", "very very long failed message. ");

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
            .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);
        when(client.createStackSet(any(CreateStackSetRequest.class)))
            .thenReturn(CREATE_STACK_SET_RESPONSE);
        when(client.createStackInstances(any(CreateStackInstancesRequest.class)))
            .thenReturn(CREATE_STACK_INSTANCES_RESPONSE);
        when(client.describeStackSetOperation(any(DescribeStackSetOperationRequest.class)))
            .thenReturn(getFailedDescribeStackSetOperationResponse(null));
        when(client.listStackSetOperationResults(any(ListStackSetOperationResultsRequest.class)))
            .thenReturn(getListStackSetOperationResultsResponse(failedStatusReason));

        Exception e = assertThrows(
            CfnNotStabilizedException.class,
            () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
        verify(client).createStackSet(any(CreateStackSetRequest.class));
        verify(client).createStackInstances(any(CreateStackInstancesRequest.class));
        verify(client).describeStackSetOperation(any(DescribeStackSetOperationRequest.class));
        verify(client).listStackSetOperationResults(any(ListStackSetOperationResultsRequest.class));
        assertThat(e.getMessage()).contains("... Use list-stack-set-operation-results for more information.");
    }

    @Test
    public void handlerRequest_CfnInvalidRequestException_NestedStack() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_DUPLICATE_INSTANCES_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(TEMPLATE_SUMMARY_RESPONSE_WITH_NESTED_STACK);

        assertThrows(
                CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
    }

    @Test
    public void handlerRequest_CfnInvalidRequestException_DuplicateStackInstance() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_DUPLICATE_INSTANCES_MODEL)
                .desiredResourceTags(DESIRED_RESOURCE_TAGS)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);

        assertThrows(
                CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
    }

    @Test
    public void handlerRequest_CfnInvalidRequestException_InvalidDeploymentTargets() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_INVALID_INSTANCES_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        when(client.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(VALID_TEMPLATE_SUMMARY_RESPONSE);

        assertThrows(
                CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, loggerProxy));

        verify(client).getTemplateSummary(any(GetTemplateSummaryRequest.class));
    }
}
