package software.amazon.cloudformation.moduledefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private CloudFormationClient client = getServiceClient();
    private DeleteHandler handler;

    protected DeleteHandlerTest() {
        super(CloudFormationClient.class);
        this.handler = new DeleteHandler();
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
    }

    @Test
    public void handleRequest_AlwaysSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
