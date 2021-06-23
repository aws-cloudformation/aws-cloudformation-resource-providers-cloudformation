package software.amazon.cloudformation.resourcedefaultversion;

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
    private ListHandler handler = new ListHandler();

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final TypeVersionSummary type = TypeVersionSummary.builder()
                .description("AWS Demo Resource")
                .type(RegistryType.RESOURCE)
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .typeName("AWS::Demo::Resource")
                .build();
        final TypeVersionSummary type2 = TypeVersionSummary.builder()
                .description("My Resource")
                .type(RegistryType.RESOURCE)
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/My-Demo-Resource/00000002")
                .typeName("My::Demo::Resource")
                .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(Arrays.asList(type, type2))
                .nextToken("token")
                .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .build();

        final ResourceModel model2 = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/My-Demo-Resource/00000002")
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
                .description("AWS Demo Resource")
                .type(RegistryType.RESOURCE)
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
                .typeName("AWS::Demo::Resource")
                .build();
        final TypeVersionSummary type2 = TypeVersionSummary.builder()
                .description("My Resource")
                .type(RegistryType.RESOURCE)
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/My-Demo-Resource/00000002")
                .typeName("My::Demo::Resource")
                .build();
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(Arrays.asList(type, type2))
                .nextToken("token")
                .build();
        when(client.listTypeVersions(ArgumentMatchers.any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource/00000001")
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
