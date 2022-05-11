package software.amazon.cloudformation.hooktypeconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsRequest;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationDetails;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String ALIAS = "default";
    private static final String CONFIGURATION_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type-configuration/hook/AWS-Demo-Hook/default";
    private static final String CONFIGURATION = "{\"CloudFormationConfiguration\":{\"HookConfiguration\":{\"TargetStacks\":\"ALL\",\"FailureMode\":\"WARN\",\"Properties\":{\"limitSize\": \"1\",\"encryptionAlgorithm\": \"aws:kms\"}}}}";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";

    private static final TypeConfigurationDetails TYPE_CONFIGURATION_DETAILS = TypeConfigurationDetails.builder()
            .arn(CONFIGURATION_ARN)
            .alias(ALIAS)
            .configuration(CONFIGURATION)
            .typeArn(TYPE_ARN)
            .build();

    private final ReadHandler handler = new ReadHandler();

    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(TYPE_CONFIGURATION_DETAILS)
                .build();
        when(client.batchDescribeTypeConfigurations(ArgumentMatchers.any(BatchDescribeTypeConfigurationsRequest.class)))
                .thenReturn(batchDescribeTypeConfigurationsResponse);

        final ResourceModel model = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .configurationArn(CONFIGURATION_ARN)
                .configuration(CONFIGURATION)
                .configurationAlias(ALIAS)
                .typeName(TYPE_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .build();

        when(client.batchDescribeTypeConfigurations(ArgumentMatchers.any(BatchDescribeTypeConfigurationsRequest.class)))
                .thenThrow(make(
                        CloudFormationException.builder(), 400, "Bad request",
                        CloudFormationException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(resourceModel);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
