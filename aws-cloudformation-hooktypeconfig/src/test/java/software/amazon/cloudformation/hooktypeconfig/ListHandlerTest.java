package software.amazon.cloudformation.hooktypeconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsRequest;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsResponse;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationDetails;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
    private static final String ALIAS_1 = "default";
    private static final String ALIAS_2 = "custom";
    private static final String CONFIGURATION_ARN_1 = "arn:aws:cloudformation:us-west-2:123456789012:type-configuration/hook/AWS-Demo-Hook/default";
    private static final String CONFIGURATION_ARN_2 = "arn:aws:cloudformation:us-west-2:123456789012:type-configuration/hook/AWS-Demo-Hook/custom";
    private static final String CONFIGURATION_1 = "{\"CloudFormationConfiguration\":{\"HookConfiguration\":{\"TargetStacks\":\"ALL\",\"FailureMode\":\"WARN\",\"Properties\":{\"limitSize\": \"1\",\"encryptionAlgorithm\": \"aws:kms\"}}}}";
    private static final String CONFIGURATION_2 = "{\"CloudFormationConfiguration\":{\"HookConfiguration\":{\"TargetStacks\":\"NONE\",\"FailureMode\":\"FAIL\",\"Properties\":{\"limitSize\": \"1\",\"encryptionAlgorithm\": \"aws:kms\"}}}}";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";


    private static final TypeConfigurationDetails TYPE_CONFIGURATION_DETAILS_1 = TypeConfigurationDetails.builder()
            .arn(CONFIGURATION_ARN_1)
            .alias(ALIAS_1)
            .configuration(CONFIGURATION_1)
            .typeArn(TYPE_ARN)
            .build();
    private static final TypeConfigurationDetails TYPE_CONFIGURATION_DETAILS_2 = TypeConfigurationDetails.builder()
            .arn(CONFIGURATION_ARN_2)
            .alias(ALIAS_2)
            .configuration(CONFIGURATION_2)
            .typeArn(TYPE_ARN)
            .build();

    private final ListHandler handler = new ListHandler();

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(Arrays.asList(TYPE_CONFIGURATION_DETAILS_1, TYPE_CONFIGURATION_DETAILS_2))
                .build();
        when(client.batchDescribeTypeConfigurations(ArgumentMatchers.any(BatchDescribeTypeConfigurationsRequest.class)))
                .thenReturn(batchDescribeTypeConfigurationsResponse);

        final ResourceModel model1 = ResourceModel.builder()
                .configurationArn(CONFIGURATION_ARN_1)
                .typeName(TYPE_NAME)
                .configurationAlias(ALIAS_1)
                .configuration(CONFIGURATION_1)
                .typeArn(TYPE_ARN)
                .build();

        final ResourceModel model2 = ResourceModel.builder()
                .configurationArn(CONFIGURATION_ARN_2)
                .typeName(TYPE_NAME)
                .configurationAlias(ALIAS_2)
                .configuration(CONFIGURATION_2)
                .typeArn(TYPE_ARN)
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
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().get(0)).isEqualTo(model1);
        assertThat(response.getResourceModels().get(1)).isEqualTo(model2);
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
