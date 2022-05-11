package software.amazon.cloudformation.hookversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String TYPE_VERSION_ARN_00000001 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String TYPE_VERSION_ARN_00000002 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000002";

    private final ListHandler handler = new ListHandler();

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final TypeVersionSummary type = TypeVersionSummary.builder()
                .description("AWS Demo Hook")
                .type(RegistryType.RESOURCE)
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();
        final TypeVersionSummary type2 = TypeVersionSummary.builder()
                .description("My Hook")
                .type(RegistryType.RESOURCE)
                .arn(TYPE_VERSION_ARN_00000002)
                .typeName(TYPE_NAME)
                .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(Arrays.asList(type, type2))
                .nextToken("token")
                .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .build();

        final ResourceModel model2 = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000002)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
        assertThat(response.getNextToken()).isEqualTo("token");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
