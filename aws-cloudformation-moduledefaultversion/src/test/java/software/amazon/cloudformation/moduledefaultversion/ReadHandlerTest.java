package software.amazon.cloudformation.moduledefaultversion;

import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private CloudFormationClient client = getServiceClient();
    private ReadHandler handler;

    private final String arn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String moduleName = "My::Test::Resource::MODULE";
    private final String versionId = "00000021";

    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
        this.handler = new ReadHandler();
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

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .deprecatedStatus(DeprecatedStatus.LIVE)
                .isDefaultVersion(true)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenReturn(describeTypeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

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

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .deprecatedStatus(DeprecatedStatus.LIVE)
                .isDefaultVersion(true)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenReturn(describeTypeResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        ArgumentCaptor<DescribeTypeRequest> captor = ArgumentCaptor.forClass(DescribeTypeRequest.class);
        verify(client, times(1)).describeType(captor.capture());
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

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .deprecatedStatus(DeprecatedStatus.LIVE)
                .isDefaultVersion(true)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenReturn(describeTypeResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        ArgumentCaptor<DescribeTypeRequest> captor = ArgumentCaptor.forClass(DescribeTypeRequest.class);
        verify(client, times(1)).describeType(captor.capture());
        assertThat(captor.getValue().arn()).isNull();
        assertThat(captor.getValue().typeAsString()).isEqualTo("MODULE");
        assertThat(captor.getValue().typeName()).isEqualTo(moduleName);
        assertThat(captor.getValue().versionId()).isEqualTo(versionId);
    }

    @Test
    public void handleRequest_ExistsButNotDefaultVersion() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .deprecatedStatus(DeprecatedStatus.LIVE)
                .isDefaultVersion(false)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenReturn(describeTypeResponse);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);

        verify(client, times(1)).describeType(any(DescribeTypeRequest.class));
    }

    @Test
    public void handleRequest_ExistsButDeprecated() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .deprecatedStatus(DeprecatedStatus.DEPRECATED)
                .isDefaultVersion(true)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenReturn(describeTypeResponse);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.describeType(any(DescribeTypeRequest.class))).thenThrow(TypeNotFoundException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_RegistryError() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        CfnRegistryException exception = CfnRegistryException.builder().build();
        when(client.describeType(any(DescribeTypeRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(exception)
                .hasMessage(null)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_NullModelProvided() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Invalid request provided: ResourceModel is required")
                .isExactlyInstanceOf(CfnInvalidRequestException.class);

        verify(client, times(0)).describeType(any(DescribeTypeRequest.class));
    }
}
