package software.amazon.cloudformation.resourceversion;

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
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
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
    private CreateHandler handler = new CreateHandler();

    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_CreateFailed() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
            .thenThrow(make(
                CfnRegistryException.builder(), 500, "some exception",
                CfnRegistryException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder().build())
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext().getPredictedArn()).isNull();
        assertThat(response.getCallbackContext().getRegistrationToken()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_NoExistingVersions_Success() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
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

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.COMPLETE)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
            .thenReturn(describeTypeRegistrationResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .visibility(Visibility.PRIVATE)
            .isDefaultVersion(false)
            .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
            .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .isDefaultVersion(false)
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .versionId("00000001")
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
    public void handleRequest_ExistingVersions_Success() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
            .registrationToken(UUID.randomUUID().toString())
            .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
            .thenReturn(registerTypeResponse);

        final TypeVersionSummary typeVersionSummary = TypeVersionSummary.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
            .typeVersionSummaries(typeVersionSummary)
            .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
            .thenReturn(listTypeVersionsResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.COMPLETE)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000002")
            .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
            .thenReturn(describeTypeRegistrationResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000002")
            .typeName("AWS::Demo::Resource")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .visibility(Visibility.PRIVATE)
            .isDefaultVersion(false)
            .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
            .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .isDefaultVersion(false)
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000002")
            .versionId("00000002") // next version
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
    public void handleRequest_ExistingVersions_PaginationHandling() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
            .registrationToken(UUID.randomUUID().toString())
            .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
            .thenReturn(registerTypeResponse);

        final TypeVersionSummary typeVersionSummary1 = TypeVersionSummary.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();
        final TypeVersionSummary typeVersionSummary2 = TypeVersionSummary.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000004")
            .build();
        final TypeVersionSummary typeVersionSummary3 = TypeVersionSummary.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000003")
            .build();
        final TypeVersionSummary typeVersionSummary4 = TypeVersionSummary.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000002")
            .build();
        final ListTypeVersionsResponse listTypeVersionsResponseFirstPage = ListTypeVersionsResponse.builder()
            .typeVersionSummaries(typeVersionSummary1, typeVersionSummary2)
            .nextToken("token")
            .build();
        final ListTypeVersionsResponse listTypeVersionsResponseLastPage = ListTypeVersionsResponse.builder()
            .typeVersionSummaries(typeVersionSummary3, typeVersionSummary4)
            .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
            .thenReturn(listTypeVersionsResponseFirstPage, listTypeVersionsResponseLastPage);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.COMPLETE)
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000005")
            .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
            .thenReturn(describeTypeRegistrationResponse);

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000005")
            .typeName("AWS::Demo::Resource")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .visibility(Visibility.PRIVATE)
            .isDefaultVersion(false)
            .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
            .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .isDefaultVersion(false)
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000005")
            .versionId("00000005") // highest version
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
    public void handleRequest_PredictArn_CfnRegistryException1() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
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

        // throw on DescribeTypeRegistration
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
            .thenThrow(make(
                CfnRegistryException.builder(), 500, "some exception",
                CfnRegistryException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext().getPredictedArn()).isEqualTo("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001");
        assertThat(response.getCallbackContext().getRegistrationToken()).isEqualTo(registerTypeResponse.registrationToken());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModelResult);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_PredictArn_CfnRegistryException() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
            .registrationToken(UUID.randomUUID().toString())
            .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
            .thenReturn(registerTypeResponse);

        // lack of existing type results in a CfnRegistryException from ListTypeVersions
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
            .thenThrow(make(
                CfnRegistryException.builder(), 500, "some exception",
                CfnRegistryException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext().getPredictedArn()).isNotNull();
        assertThat(response.getCallbackContext().getRegistrationToken()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_StabilizeFailed() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
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
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .build();
        when(client.describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class)))
            .thenReturn(describeTypeRegistrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
            .hasNoCause()
            .hasMessage("Resource of type 'AWS::CloudFormation::ResourceVersion' with identifier 'arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001' did not stabilize.")
            .isExactlyInstanceOf(CfnNotStabilizedException.class);
    }

    @Test
    // Test hangs because the mock chain for DescribeTypeRegistration keeps using IN_PROGRESS result for some reason
    public void handleRequest_DelayedCompletion() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
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

        // First registration IN_PROGRESS to force a stabilization loop
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseInProgress = DescribeTypeRegistrationResponse.builder()
            .progressStatus(RegistrationStatus.IN_PROGRESS)
            .build();
        // Second request is COMPLETE
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseComplete = DescribeTypeRegistrationResponse.builder()
            .typeVersionArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000002")
            .progressStatus(RegistrationStatus.COMPLETE)
            .build();
        doReturn(describeTypeRegistrationResponseInProgress, describeTypeRegistrationResponseComplete)
            .when(client).describeTypeRegistration(ArgumentMatchers.any(DescribeTypeRegistrationRequest.class));

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .typeName("AWS::Demo::Resource")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .visibility(Visibility.PRIVATE)
            .isDefaultVersion(false)
            .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
            .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(resourceModel)
            .awsPartition("aws")
            .region("us-west-2")
            .awsAccountId("123456789012")
            .build();

        final ResourceModel resourceModelResult = ResourceModel.builder()
            .typeName("AWS::Demo::Resource")
            .visibility("PRIVATE")
            .sourceUrl("https://github.com/myorg/resource/repo.git")
            .isDefaultVersion(false)
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
            .versionId("00000001") // next version
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
    public void handleRequest_PredictArn_OtherException() {
        final CloudFormationClient client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName("AWS::Demo::Resource")
                .visibility("PRIVATE")
                .sourceUrl("https://github.com/myorg/resource/repo.git")
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(ArgumentMatchers.any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        // lack of existing type results in a CfnRegistryException from ListTypeVersions
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenThrow(NullPointerException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition("aws")
                .region("us-west-2")
                .awsAccountId("123456789012")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext().getPredictedArn()).isNull();
        assertThat(response.getCallbackContext().getRegistrationToken()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
