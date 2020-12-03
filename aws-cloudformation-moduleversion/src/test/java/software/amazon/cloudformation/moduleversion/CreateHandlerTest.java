package software.amazon.cloudformation.moduleversion;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private CloudFormationClient client = getServiceClient();
    private CreateHandler handler;
    private ReadHandler readHandler;
    private ArnPredictor arnPredictor;

    private final String arn              = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String description      = "This is a test model.";
    private final String documentationUrl = "https://documentation-url-test-value/";
    private final String moduleName       = "My::Test::Resource::MODULE";
    private final String modulePackage    = "s3://test-module-package/";
    private final String versionId        = "00000021";

    private final String registrationToken = UUID.randomUUID().toString();

    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
        this.readHandler = mock(ReadHandler.class);
        this.arnPredictor = mock(ArnPredictor.class);
        this.handler = new CreateHandler(this.readHandler, this.arnPredictor);
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
        when(this.arnPredictor.predictArn(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(arn);
    }

    @Test
    public void handleRequest_BasicSuccess() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(registrationToken)
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.COMPLETE)
                .typeVersionArn(arn)
                .build();
        when(client.describeTypeRegistration(any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelOut)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        ArgumentCaptor<CallbackContext> captor = ArgumentCaptor.forClass(CallbackContext.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verify(readHandler, times(1))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), captor.capture(), any(), any(Logger.class));
        assertEquals(registrationToken, captor.getValue().getRegistrationToken());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(modelOut);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Stabilization_Delayed() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(registrationToken)
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseInProgress = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.IN_PROGRESS)
                .typeVersionArn(arn)
                .build();
        final DescribeTypeRegistrationResponse describeTypeRegistrationResponseComplete = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.COMPLETE)
                .typeVersionArn(arn)
                .build();
        doReturn(
                describeTypeRegistrationResponseInProgress,
                describeTypeRegistrationResponseComplete
        ).when(client).describeTypeRegistration(any(DescribeTypeRegistrationRequest.class));

        ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelOut)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        ArgumentCaptor<CallbackContext> captor = ArgumentCaptor.forClass(CallbackContext.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verify(readHandler, times(1))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), captor.capture(), any(), any(Logger.class));
        assertEquals(registrationToken, captor.getValue().getRegistrationToken());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(modelOut);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Stabilization_Failed() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.FAILED)
                .typeVersionArn(arn)
                .build();
        when(client.describeTypeRegistration(any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + arn + "' did not stabilize.")
                .isExactlyInstanceOf(CfnNotStabilizedException.class);

        verify(readHandler, times(0))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_Stabilization_UnknownStatus() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus("unknown")
                .typeVersionArn(arn)
                .build();
        when(client.describeTypeRegistration(any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'received unexpected module registration status: null'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);

        verify(readHandler, times(0))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_Read_GeneralError() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(registrationToken)
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final DescribeTypeRegistrationResponse describeTypeRegistrationResponse = DescribeTypeRegistrationResponse.builder()
                .progressStatus(RegistrationStatus.COMPLETE)
                .typeVersionArn(arn)
                .build();
        when(client.describeTypeRegistration(any(DescribeTypeRegistrationRequest.class)))
                .thenReturn(describeTypeRegistrationResponse);

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(new CfnGeneralServiceException("test"));

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'test'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);

        verify(readHandler, times(1))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_CheckRegistration_RegistryError() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final RegisterTypeResponse registerTypeResponse = RegisterTypeResponse.builder()
                .registrationToken(UUID.randomUUID().toString())
                .build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenReturn(registerTypeResponse);

        final CfnRegistryException registryException = CfnRegistryException.builder().build();
        when(client.describeTypeRegistration(any(DescribeTypeRegistrationRequest.class)))
                .thenThrow(registryException);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(registryException)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_Register_RegistryError() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final CfnRegistryException registryException = CfnRegistryException.builder().build();
        when(client.registerType(any(RegisterTypeRequest.class)))
                .thenThrow(registryException);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(registryException)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_BadInput_NullModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Invalid request provided: ResourceModel is required")
                .isExactlyInstanceOf(CfnInvalidRequestException.class);
    }

    @Test
    public void handleRequest_Failure_ArnPrediction() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .moduleName(moduleName)
                .modulePackage(modulePackage)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(this.arnPredictor.predictArn(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage(String.format("Error occurred during operation 'ARN prediction for new module version of module %s'.", moduleName))
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }
}
