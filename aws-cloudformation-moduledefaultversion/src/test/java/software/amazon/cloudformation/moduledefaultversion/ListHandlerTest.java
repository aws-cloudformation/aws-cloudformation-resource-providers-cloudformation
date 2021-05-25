package software.amazon.cloudformation.moduledefaultversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeSummary;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private static final String MODULE_ARN =
            "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE";
    private static final String MODULE_NAME = "My::Test::Resource::MODULE";
    private static final String RESOURCE_NAME = "My::Test::Resource";
    private static final String RESOURCE_ARN = "arn:aws:s3:::updaterollbackbucket";
    private static final String NEXT_TOKEN = "DUMMY_NEXT_TOKEN";
    private static final String DEFAULT_VERSION_ID = "00000001";

    private CloudFormationClient client = getServiceClient();
    private ListHandler handler;

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
        this.handler = new ListHandler();
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final TypeSummary moduleTypeSummary = TypeSummary.builder()
                .defaultVersionId(DEFAULT_VERSION_ID)
                .type("MODULE")
                .typeArn(MODULE_ARN)
                .typeName(MODULE_NAME)
                .build();
        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(moduleTypeSummary)
                .nextToken(NEXT_TOKEN)
                .build();
        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(MODULE_ARN)
                .moduleName(MODULE_NAME)
                .versionId(DEFAULT_VERSION_ID)
                .build();
        when(client.listTypes(any(ListTypesRequest.class))).thenReturn(listTypesResponse);
        final ResourceModel model = ResourceModel.builder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model)
                        .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotEmpty();
        assertThat(response.getNextToken()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        List<ResourceModel> resourceModels = response.getResourceModels();
        ResourceModel actual = resourceModels.get(0);
        assertThat(actual).isEqualTo(expectedResourceModel);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN);
    }

    @Test
    public void handleRequest_ShouldReturnOnlyModules() {
        final TypeSummary resourceTypeSummary = TypeSummary.builder()
                .defaultVersionId(DEFAULT_VERSION_ID)
                .type("RESOURCE")
                .typeArn(RESOURCE_ARN)
                .typeName(RESOURCE_NAME)
                .build();
        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(resourceTypeSummary)
                .build();
        final ResourceModel model = ResourceModel.builder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model)
                        .nextToken(NEXT_TOKEN)
                        .build();
        when(client.listTypes(any(ListTypesRequest.class))).thenReturn(listTypesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEmpty();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
