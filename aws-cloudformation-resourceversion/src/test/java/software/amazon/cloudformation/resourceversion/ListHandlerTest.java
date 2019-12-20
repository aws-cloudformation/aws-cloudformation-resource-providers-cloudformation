package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {
    software.amazon.cloudformation.resourceversion.ListHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new software.amazon.cloudformation.resourceversion.ListHandler();
    }

    @Test
    public void handleRequest_Success() {
        final TypeSummary type = TypeSummary.builder()
            .defaultVersionId("00000001")
            .description("AWS Demo Resource")
            .lastUpdated(Instant.ofEpochSecond(123456789012L))
            .type(RegistryType.RESOURCE)
            .typeArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource")
            .typeName("AWS::Demo::Resource")
            .build();
        final TypeSummary type2 = TypeSummary.builder()
            .defaultVersionId("00000007")
            .description("My Resource")
            .lastUpdated(Instant.ofEpochSecond(923456789012L))
            .type(RegistryType.RESOURCE)
            .typeArn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/My-Demo-Resource")
            .typeName("My::Demo::Resource")
            .build();
        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
            .typeSummaries(Arrays.asList(type, type2))
            .nextToken("token2")
            .build();

        doReturn(listTypesResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model1 = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Demo-Resource")
            .build();

        final ResourceModel model2 = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/My-Demo-Resource")
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .nextToken("token")
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
        assertThat(response.getNextToken()).isEqualTo("token2");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
