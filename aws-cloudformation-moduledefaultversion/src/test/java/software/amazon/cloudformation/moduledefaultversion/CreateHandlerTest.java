package software.amazon.cloudformation.moduledefaultversion;

import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private CloudFormationClient client = getServiceClient();
    private CreateHandler handler;
    private ReadHandler readHandler;

    private final String arn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String moduleName = "My::Test::Resource::MODULE";
    final String versionId = "00000021";

    protected CreateHandlerTest() {
        super(CloudFormationClient.class);
        this.readHandler = mock(ReadHandler.class);
        this.handler = new CreateHandler(readHandler);
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
    }

    @Test
    public void handleRequest_BasicSuccess() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(modelIn)
            .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(new CfnNotFoundException(ResourceModel.TYPE_NAME, modelIn.getPrimaryIdentifier().toString()));

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verifyZeroInteractions(readHandler);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(modelOut);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Translation_ArnInput() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(new CfnNotFoundException(ResourceModel.TYPE_NAME, modelIn.getPrimaryIdentifier().toString()));

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        ArgumentCaptor<SetTypeDefaultVersionRequest> captor = ArgumentCaptor.forClass(SetTypeDefaultVersionRequest.class);
        verify(client, times(1)).setTypeDefaultVersion(captor.capture());
        assertThat(captor.getValue().arn()).isEqualTo(arn);
        assertThat(captor.getValue().type()).isNull();
        assertThat(captor.getValue().typeName()).isNull();
        assertThat(captor.getValue().versionId()).isNull();
    }

    @Test
    public void handleRequest_Translation_NameAndVersionInput() {
        final ResourceModel modelIn = ResourceModel.builder()
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder().resourceModel(modelOut).build());

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        ArgumentCaptor<SetTypeDefaultVersionRequest> captor = ArgumentCaptor.forClass(SetTypeDefaultVersionRequest.class);
        verify(client, times(1)).setTypeDefaultVersion(captor.capture());
        assertThat(captor.getValue().arn()).isNull();
        assertThat(captor.getValue().typeAsString()).isEqualTo("MODULE");
        assertThat(captor.getValue().typeName()).isEqualTo(moduleName);
        assertThat(captor.getValue().versionId()).isEqualTo(versionId);
    }

    @Test
    public void handleRequest_SetDefaultVersion_NotFound_WithArnInput() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);

        verify(client, times(1)).setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class));
    }

    @Test
    public void handleRequest_SetDefaultVersion_NotFound_ModuleNameVersionIdInput() {
        final ResourceModel modelIn = ResourceModel.builder()
                .moduleName("foo")
                .versionId("00001")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getModuleName() + " v" + modelIn.getVersionId() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);

        verify(client, times(1)).setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class));
    }

    @Test
    public void handleRequest_SetDefaultVersion_RegistryError() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final CfnRegistryException registryException = CfnRegistryException.builder().build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenThrow(registryException);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(registryException)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);

        verifyZeroInteractions(readHandler);
        verify(client, times(1)).setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class));
    }

    @Test
    public void handleRequest_FinalRead_NotCalled() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        verifyZeroInteractions(readHandler);
    }

    @Test
    public void handleRequest_FinalRead_Called() {
        final ResourceModel modelIn = ResourceModel.builder()
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder().resourceModel(modelOut).build());

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        verify(readHandler, times(1))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_FinalRead_Error() {
        final ResourceModel modelIn = ResourceModel.builder()
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(readHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(new CfnGeneralServiceException("test"));

        final SetTypeDefaultVersionResponse setTypeDefaultVersionResponse = SetTypeDefaultVersionResponse.builder()
                .build();
        when(client.setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class)))
                .thenReturn(setTypeDefaultVersionResponse);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'test'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_BadInput_NullModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Invalid request provided: ResourceModel is required")
                .isExactlyInstanceOf(CfnInvalidRequestException.class);

        verify(readHandler, times(0))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        verify(client, times(0)).setTypeDefaultVersion(any(SetTypeDefaultVersionRequest.class));
    }
}
