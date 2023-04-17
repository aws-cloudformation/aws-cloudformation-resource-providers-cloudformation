package software.amazon.cloudformation.moduleversion;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private CloudFormationClient client = getServiceClient();
    private ReadHandler handler = new ReadHandler();

    private final String arn              = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String description      = "This is a test model.";
    private final String documentationUrl = "https://documentation-url-test-value/";
    private final String moduleName       = "My::Test::Resource::MODULE";
    private final String versionId        = "00000021";

    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
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

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceModel modelOut = ResourceModel.builder()
                .arn(arn)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat((Object)response.getResourceModel()).isEqualToComparingFieldByField(modelOut);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RegistryError() {
        final ResourceModel model = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenThrow(CfnRegistryException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCauseExactlyInstanceOf(CfnRegistryException.class)
                .hasMessage(null)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_DeprecatedModule() {
        final boolean isDefaultVersion = false;

        final ResourceModel modelIn = ResourceModel.builder()
                .arn(arn)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .defaultVersionId(versionId)
                .deprecatedStatus(DeprecatedStatus.DEPRECATED)
                .description(description)
                .documentationUrl(documentationUrl)
                .isDefaultVersion(isDefaultVersion)
                .type("MODULE")
                .typeName(moduleName)
                .build();
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelIn)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + modelIn.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
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
