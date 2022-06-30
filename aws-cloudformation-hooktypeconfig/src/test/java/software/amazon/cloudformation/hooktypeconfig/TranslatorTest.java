package software.amazon.cloudformation.hooktypeconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsError;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsRequest;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeConfigurationRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationDetails;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationIdentifier;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.LoggerProxy;

import java.util.List;
import org.json.JSONObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String ALIAS = "custom";
    private static final String CONFIGURATION_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type-configuration/hook/AWS-Demo-Hook/default";
    private static final String CONFIGURATION = "{\"CloudFormationConfiguration\":{\"HookConfiguration\":{\"TargetStacks\":\"ALL\",\"FailureMode\":\"WARN\",\"Properties\":{\"limitSize\": \"1\",\"encryptionAlgorithm\": \"aws:kms\"}}}}";
    private static final String CONFIGURATION_DISABLED = "{\"CloudFormationConfiguration\":{\"HookConfiguration\":{\"TargetStacks\":\"NONE\",\"Properties\":{\"limitSize\":\"1\",\"encryptionAlgorithm\":\"aws:kms\"},\"FailureMode\":\"WARN\"}}}";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";
    private static final String TYPE = "HOOK";

    private static final TypeConfigurationDetails TYPE_CONFIGURATION_DETAILS = TypeConfigurationDetails.builder()
            .arn(CONFIGURATION_ARN)
            .alias(ALIAS)
            .configuration(CONFIGURATION)
            .typeArn(TYPE_ARN)
            .build();

    @Test
    public void translateToUpdateRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToUpdateRequest(null))
                .hasNoCause()
                .hasMessageEndingWith("but is null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToUpdateRequest_typeName() {
        ResourceModel model = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .configuration(CONFIGURATION)
                .build();
        SetTypeConfigurationRequest request = Translator.translateToUpdateRequest(model);

        assertThat(request.configuration()).isEqualTo(CONFIGURATION);
        assertThat(request.typeName()).isEqualTo(TYPE_NAME);
        assertThat(request.typeAsString()).isEqualTo(TYPE);
    }

    @Test
    public void translateToUpdateRequest_Arn() {
        ResourceModel model = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .configuration(CONFIGURATION)
                .configurationAlias(ALIAS)
                .build();
        SetTypeConfigurationRequest request = Translator.translateToUpdateRequest(model);

        assertThat(request.configuration()).isEqualTo(CONFIGURATION);
        assertThat(request.typeArn()).isEqualTo(TYPE_ARN);
        assertThat(request.configurationAlias()).isEqualTo(ALIAS);
    }

    @Test
    public void translateToReadRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null))
                .hasNoCause()
                .hasMessageEndingWith("but is null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_typeName() {
        ResourceModel model = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .build();
        BatchDescribeTypeConfigurationsRequest request = Translator.translateToReadRequest(model);

        assertThat(request.typeConfigurationIdentifiers()).isNotNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeName()).isEqualTo(TYPE_NAME);
        assertThat(request.typeConfigurationIdentifiers().get(0).typeAsString()).isEqualTo(TYPE);
    }

    @Test
    public void translateToReadRequest_Arn() {
        ResourceModel model = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .configurationAlias(ALIAS)
                .build();
        BatchDescribeTypeConfigurationsRequest request = Translator.translateToReadRequest(model);

        assertThat(request.typeConfigurationIdentifiers()).isNotNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeArn()).isEqualTo(TYPE_ARN);
        assertThat(request.typeConfigurationIdentifiers().get(0).typeConfigurationAlias()).isEqualTo(ALIAS);
    }

    @Test
    public void translateToReadRequest_ConfigurationArn() {
        ResourceModel model = ResourceModel.builder()
                .configurationArn(CONFIGURATION_ARN)
                .configurationAlias(ALIAS)
                .build();
        BatchDescribeTypeConfigurationsRequest request = Translator.translateToReadRequest(model);

        assertThat(request.typeConfigurationIdentifiers()).isNotNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeConfigurationArn()).isEqualTo(CONFIGURATION_ARN);
        assertThat(request.typeConfigurationIdentifiers().get(0).typeConfigurationAlias()).isEqualTo(ALIAS);
    }

    @Test
    public void translateFromRead_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateFromReadResponse(null, null))
                .hasNoCause()
                .hasMessageEndingWith("but is null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateFromRead_Error() {
        BatchDescribeTypeConfigurationsError error = BatchDescribeTypeConfigurationsError.builder()
                .errorMessage("TypeConfiguration not found")
                .errorCode("404")
                .typeConfigurationIdentifier(TypeConfigurationIdentifier.builder()
                        .typeArn(TYPE_ARN)
                        .build())
                .build();
        BatchDescribeTypeConfigurationsResponse response = BatchDescribeTypeConfigurationsResponse.builder()
                .errors(error)
                .build();

        assertThatThrownBy(() -> Translator.translateFromReadResponse(response, new LoggerProxy()))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'TypeConfiguration not found'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void translateFromRead() {
        BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(TYPE_CONFIGURATION_DETAILS)
                .build();
        ResourceModel model = Translator.translateFromReadResponse(batchDescribeTypeConfigurationsResponse, new LoggerProxy());

        assertThat(model.getConfigurationAlias()).isEqualTo(ALIAS);
        assertThat(model.getConfiguration()).isEqualTo(CONFIGURATION);
        assertThat(model.getTypeArn()).isEqualTo(TYPE_ARN);
        assertThat(model.getConfigurationArn()).isEqualTo(CONFIGURATION_ARN);
        assertThat(model.getTypeName()).isEqualTo(TYPE_NAME);
    }

    @Test
    public void translateFromList_Error() {
        BatchDescribeTypeConfigurationsError error = BatchDescribeTypeConfigurationsError.builder()
                .errorMessage("TypeConfiguration not found")
                .errorCode("404")
                .typeConfigurationIdentifier(TypeConfigurationIdentifier.builder()
                        .typeArn(TYPE_ARN)
                        .build())
                .build();
        BatchDescribeTypeConfigurationsResponse response = BatchDescribeTypeConfigurationsResponse.builder()
                .errors(error)
                .build();

        assertThatThrownBy(() -> Translator.translateFromListResponse(response, new LoggerProxy()))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'TypeConfiguration not found'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void translateFromList() {
        BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse = BatchDescribeTypeConfigurationsResponse.builder()
                .typeConfigurations(TYPE_CONFIGURATION_DETAILS)
                .build();
        List<ResourceModel> models = Translator.translateFromListResponse(batchDescribeTypeConfigurationsResponse, new LoggerProxy());

        assertThat(models.size()).isEqualTo(1);
        assertThat(models.get(0).getConfigurationAlias()).isEqualTo(ALIAS);
        assertThat(models.get(0).getConfiguration()).isEqualTo(CONFIGURATION);
        assertThat(models.get(0).getTypeArn()).isEqualTo(TYPE_ARN);
        assertThat(models.get(0).getConfigurationArn()).isEqualTo(CONFIGURATION_ARN);
        assertThat(models.get(0).getTypeName()).isEqualTo(TYPE_NAME);
    }

    @Test
    public void translateToListRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToListRequest(null))
                .hasNoCause()
                .hasMessageEndingWith("but is null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToListRequest_typeName() {
        ResourceModel model = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .build();
        BatchDescribeTypeConfigurationsRequest request = Translator.translateToListRequest(model);

        assertThat(request.typeConfigurationIdentifiers()).isNotNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeName()).isEqualTo(TYPE_NAME);
        assertThat(request.typeConfigurationIdentifiers().get(0).typeConfigurationAlias()).isNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeAsString()).isEqualTo(TYPE);
    }

    @Test
    public void translateToListRequest_Arn() {
        ResourceModel model = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .configurationAlias(ALIAS)
                .build();
        BatchDescribeTypeConfigurationsRequest request = Translator.translateToListRequest(model);

        assertThat(request.typeConfigurationIdentifiers()).isNotNull();
        assertThat(request.typeConfigurationIdentifiers().get(0).typeArn()).isEqualTo(TYPE_ARN);
        assertThat(request.typeConfigurationIdentifiers().get(0).typeConfigurationAlias()).isNull();
    }

    @Test
    public void translateToDeleteRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToDeleteRequest(null))
                .hasNoCause()
                .hasMessageEndingWith("but is null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToDeleteRequest_typeName() {
        ResourceModel model = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .configuration(CONFIGURATION)
                .build();
        SetTypeConfigurationRequest request = Translator.translateToDeleteRequest(model);

        assertThat(request.configuration()).isEqualTo(CONFIGURATION_DISABLED);
        assertThat(request.typeName()).isEqualTo(TYPE_NAME);
        assertThat(request.typeAsString()).isEqualTo(TYPE);
    }

    @Test
    public void translateToDeleteRequest_Arn() {
        ResourceModel model = ResourceModel.builder()
                .typeArn(TYPE_ARN)
                .configuration(CONFIGURATION)
                .configurationAlias(ALIAS)
                .build();
        SetTypeConfigurationRequest request = Translator.translateToDeleteRequest(model);

        assertThat(request.configuration()).isEqualTo(CONFIGURATION_DISABLED);
        assertThat(request.typeArn()).isEqualTo(TYPE_ARN);
        assertThat(request.configurationAlias()).isEqualTo(ALIAS);
    }

}
