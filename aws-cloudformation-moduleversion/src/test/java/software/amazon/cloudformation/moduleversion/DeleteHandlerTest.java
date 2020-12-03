package software.amazon.cloudformation.moduleversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private DeleteHandler handler;
    private CloudFormationClient client = getServiceClient();
    private ReadHandler readHandler;

    private final String arn              = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String description      = "This is a test model.";
    private final String documentationUrl = "https://documentation-url-test-value/";
    private final String moduleName       = "My::Test::Resource::MODULE";
    private final String versionId        = "00000021";

    protected DeleteHandlerTest() {
        super(CloudFormationClient.class);
        this.readHandler = mock(ReadHandler.class);
        this.handler = new DeleteHandler(readHandler);
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
    }

    @Test
    public void handleRequest_BasicSuccess() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        final ResourceModel modelAfterRead = ResourceModel
                .builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelAfterRead)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.deregisterType(any(DeregisterTypeRequest.class)))
                .thenReturn(DeregisterTypeResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    void handleRequest_Deregistration_GeneralError() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        final ResourceModel modelAfterRead = ResourceModel
                .builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelAfterRead)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.deregisterType(any(DeregisterTypeRequest.class)))
                .thenThrow(new CfnGeneralServiceException("module deregistration"));

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'module deregistration'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    void handleRequest_Read_GeneralError() {
        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(new CfnGeneralServiceException("module read"));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'module read'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    void handleRequest_NotFound() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        final ResourceModel modelAfterRead = ResourceModel
                .builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelAfterRead)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final CfnNotFoundException exception = new CfnNotFoundException(ResourceModel.TYPE_NAME, modelAfterRead.getPrimaryIdentifier().toString());
        when(client.deregisterType(any(DeregisterTypeRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelAfterRead.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    void handleRequest_VersionIsDefaultVersion_OnlyVersion() {
        final boolean isDefaultVersion = true;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        final ResourceModel modelAfterRead = ResourceModel
                .builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelAfterRead)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();
        when(client.deregisterType(any(DeregisterTypeRequest.class)))
                .thenReturn(DeregisterTypeResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    void handleRequest_VersionIsDefaultVersion_NotOnlyVersion() {
        final boolean isDefaultVersion = true;

        final ResourceModel modelIn = ResourceModel
                .builder()
                .arn(arn)
                .build();

        final ResourceModel modelAfterRead = ResourceModel
                .builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(modelAfterRead)
                .status(OperationStatus.SUCCESS)
                .build();
        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(progress);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();
        final CfnRegistryException exception = CfnRegistryException.builder().build();
        when(client.deregisterType(any(DeregisterTypeRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(exception)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_BadInput_NullModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Invalid request provided: ResourceModel is required")
                .isExactlyInstanceOf(CfnInvalidRequestException.class);
    }
}
