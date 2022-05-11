package software.amazon.cloudformation.hookdefaultversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
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
    public static final String DESCRIPTION_1 = "AWS Demo Hook";
    public static final String DESCRIPTION_2 = "My Hook";
    public static final String TYPE = "HOOK";
    public static final String ARN_1 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    public static final String ARN_2 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/My-Demo-Hook/00000002";
    public static final String TYPE_NAME_1 = "AWS::Demo::Resource";
    public static final String TYPE_NAME_2 = "My::Demo::Hook";

    private final ListHandler handler = new ListHandler();

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final TypeVersionSummary type = TypeVersionSummary.builder()
                .description(DESCRIPTION_1)
                .type(TYPE)
                .arn(ARN_1)
                .typeName(TYPE_NAME_1)
                .build();
        final TypeVersionSummary type2 = TypeVersionSummary.builder()
                .description(DESCRIPTION_2)
                .type(TYPE)
                .arn(ARN_2)
                .typeName(TYPE_NAME_2)
                .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(Arrays.asList(type, type2))
                .nextToken("token")
                .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .arn(ARN_1)
                .build();

        final ResourceModel model2 = ResourceModel.builder()
                .arn(ARN_2)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getNextToken()).isEqualTo("token");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Success_withDesiredResourceState() {
        final CloudFormationClient client = getServiceClient();

        final TypeVersionSummary type = TypeVersionSummary.builder()
                .description(DESCRIPTION_1)
                .type(TYPE)
                .arn(ARN_1)
                .typeName(TYPE_NAME_1)
                .build();
        final TypeVersionSummary type2 = TypeVersionSummary.builder()
                .description(DESCRIPTION_2)
                .type(TYPE)
                .arn(ARN_2)
                .typeName(TYPE_NAME_2)
                .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(Arrays.asList(type, type2))
                .nextToken("token")
                .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .arn(ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model1)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getNextToken()).isEqualTo("token");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
