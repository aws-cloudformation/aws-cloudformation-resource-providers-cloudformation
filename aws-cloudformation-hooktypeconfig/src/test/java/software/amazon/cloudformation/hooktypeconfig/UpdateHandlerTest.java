package software.amazon.cloudformation.hooktypeconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsRequest;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeConfigurationRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeConfigurationResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationDetails;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
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

    private final UpdateHandler handler = new UpdateHandler();

    protected UpdateHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_TypeName_Success() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .build();

        final SetTypeConfigurationResponse setTypeConfigurationResponse = SetTypeConfigurationResponse.builder()
                .build();
        when(client.setTypeConfiguration(ArgumentMatchers.any(SetTypeConfigurationRequest.class)))
                .thenReturn(setTypeConfigurationResponse);
        final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(TYPE_CONFIGURATION_DETAILS)
                .build();
        when(client.batchDescribeTypeConfigurations(ArgumentMatchers.any(BatchDescribeTypeConfigurationsRequest.class)))
                .thenReturn(batchDescribeTypeConfigurationsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .typeName(TYPE_NAME)
                .configurationArn(CONFIGURATION_ARN)
                .configuration(CONFIGURATION)
                .configurationAlias(ALIAS)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_TypeArn_Success() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .build();

        final SetTypeConfigurationResponse setTypeConfigurationResponse = SetTypeConfigurationResponse.builder()
                .build();
        when(client.setTypeConfiguration(ArgumentMatchers.any(SetTypeConfigurationRequest.class)))
                .thenReturn(setTypeConfigurationResponse);
        final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(TYPE_CONFIGURATION_DETAILS)
                .build();
        when(client.batchDescribeTypeConfigurations(ArgumentMatchers.any(BatchDescribeTypeConfigurationsRequest.class)))
                .thenReturn(batchDescribeTypeConfigurationsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        final ResourceModel resourceModelResult = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .typeName(TYPE_NAME)
                .configurationArn(CONFIGURATION_ARN)
                .configuration(CONFIGURATION)
                .configurationAlias(ALIAS)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(resourceModelResult);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .build();

        when(client.setTypeConfiguration(ArgumentMatchers.any(SetTypeConfigurationRequest.class)))
                .thenThrow(make(
                        TypeNotFoundException.builder(), 404, "Type not found",
                        TypeNotFoundException.class));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();


        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_PhysicalIdChange() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .configurationArn(TYPE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> {
            final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);
        }).isInstanceOf(CfnGeneralServiceException.class)
                .hasMessageContaining("Primary Id for this resource is changed. To fix your stack, please remove this resource from the stack, perform stack update operation. Then, re-add this resource to continue regular operations of your stack. This is a one time change.");
    }
}
