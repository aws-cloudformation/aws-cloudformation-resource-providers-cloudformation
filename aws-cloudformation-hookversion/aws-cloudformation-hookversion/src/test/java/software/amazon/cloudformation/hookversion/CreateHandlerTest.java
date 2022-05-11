package software.amazon.cloudformation.hookversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.Visibility;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private static final String CLOUDFORMATION = "cloudformation";
    private static final String AWS_PARTITION = "aws";
    private static final String REGION = "us-west-2";
    private static final String AWS_ACCOUNT_ID = "123456789012";
    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String VISIBILITY = "PRIVATE";
    private static final String TYPE_VERSION_ARN_00000001 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";
    private static final String RESOURCE_REPO_GIT = "https://github.com/myorg/hook/repo.git";
    private static final String VERSION_ID = "00000001";
    private static final String TYPE_VERSION_ARN_00000002 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000002";

    private final CreateHandler handler = new CreateHandler();

    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_CreateFailed() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn(CLOUDFORMATION);

        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenThrow(make(
                        CfnRegistryException.builder(), 500, "some exception",
                        CfnRegistryException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().build())
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext().getRegistrationToken()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_simple_Success() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn(CLOUDFORMATION);

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.COMPLETE)
                .typeVersionArn(TYPE_VERSION_ARN_00000001)
                .typeArn(TYPE_ARN)
                .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .sourceUrl(RESOURCE_REPO_GIT)
                .visibility(Visibility.PRIVATE)
                .isDefaultVersion(false)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .typeArn(TYPE_ARN)
                .visibility(VISIBILITY)
                .isDefaultVersion(false)
                .arn(TYPE_VERSION_ARN_00000001)
                .versionId(VERSION_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModelResult);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handleRequest_StabilizeFailed() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn(CLOUDFORMATION);

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        // lack of existing type results in a CfnRegistryException from ListTypeVersions
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenThrow(make(
                        CfnRegistryException.builder(), 404, "Type not found",
                        CfnRegistryException.class));

        // Registration failure
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.FAILED)
                .typeVersionArn(TYPE_VERSION_ARN_00000001)
                .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type 'AWS::CloudFormation::HookVersion' with identifier '"+ TYPE_VERSION_ARN_00000001 +"' did not stabilize.")
                .isExactlyInstanceOf(CfnNotStabilizedException.class);
    }

    @Test
    // Test hangs because the mock chain for DescribeTypeRegistration keeps using IN_PROGRESS result for some reason
    public void handleRequest_DelayedCompletion() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn(CLOUDFORMATION);

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        // First registration IN_PROGRESS to force a stabilization loop
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseInProgress = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.IN_PROGRESS)
                .build();
        // Second request is COMPLETE
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseComplete = DescribeTypeRegistrationResponse.builder()
                .typeVersionArn(TYPE_VERSION_ARN_00000002)
                .progressStatus(RegistrationStatus.COMPLETE)
                .build();
        doReturn(describeTypeRegistrationResponseInProgress, describeTypeRegistrationResponseComplete)
                .when(client).describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class));

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .sourceUrl(RESOURCE_REPO_GIT)
                .visibility(Visibility.PRIVATE)
                .isDefaultVersion(false)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .typeArn(TYPE_ARN)
                .visibility(VISIBILITY)
                .isDefaultVersion(false)
                .arn(TYPE_VERSION_ARN_00000001)
                .versionId(VERSION_ID) // next version
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModelResult);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_noDescribeTypeRegistration_shouldFail() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn(CLOUDFORMATION);

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(null);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Internal error occurred.");

    }
}
