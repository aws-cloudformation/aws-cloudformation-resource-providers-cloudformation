package software.amazon.cloudformation.hookversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.ProvisioningType;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.awssdk.services.cloudformation.model.Visibility;
import software.amazon.cloudformation.proxy.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String TYPE = "HOOK";
    private static final String TYPE_VERSION_ARN_00000001 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String TYPE_VERSION_ARN_00000002 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000002";
    private static final String RESOURCE_REPO_GIT = "https://github.com/myorg/hook/repo.git";
    private static final String VERSION_ID = "00000001";
    public static final String EXECUTION_ROLE_ARN = "arn:aws:iam::123456789012:role/AppRole";
    public static final String SCHEMA_HANDLER_PACKAGE = "example-bucket/some/path/code.zip";
    public static final String DESCRIPTION = "some resource";
    public static final String DOCUMENTATION_URL = "https://mydocs.org/some-resource";
    public static final String LOG_GROUP_NAME = "my-group";
    public static final String LOG_ROLE_ARN = "arn:aws:iam::123456789012:role/LoggingRole";
    public static final String SCHEMA = "{ schema }";

    @Mock
    private Logger logger;

    @Test
    public void translateToCreateRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToCreateRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToCreateRequest_nullLoggingConfig() {
        ResourceModel model = ResourceModel.builder()
                .executionRoleArn(EXECUTION_ROLE_ARN)
                .schemaHandlerPackage(SCHEMA_HANDLER_PACKAGE)
                .typeName(TYPE_NAME)
                .build();
        RegisterTypeRequest request = Translator.translateToCreateRequest(model);

        assertThat(request.executionRoleArn()).isEqualTo(model.getExecutionRoleArn());
        assertThat(request.loggingConfig()).isNull();
        assertThat(request.schemaHandlerPackage()).isEqualTo(model.getSchemaHandlerPackage());
        assertThat(request.typeAsString()).isEqualTo(TYPE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToCreateRequest_withLoggingConfig() {
        LoggingConfig loggingConfig = LoggingConfig.builder()
                .logGroupName(LOG_GROUP_NAME)
                .logRoleArn(LOG_ROLE_ARN)
                .build();

        ResourceModel model = ResourceModel.builder()
                .executionRoleArn(EXECUTION_ROLE_ARN)
                .loggingConfig(loggingConfig)
                .schemaHandlerPackage(SCHEMA_HANDLER_PACKAGE)
                .typeName(TYPE_NAME)
                .build();
        RegisterTypeRequest request = Translator.translateToCreateRequest(model);

        assertThat(request.executionRoleArn()).isEqualTo(model.getExecutionRoleArn());
        assertThat(request.loggingConfig().logGroupName()).isEqualTo(loggingConfig.getLogGroupName());
        assertThat(request.loggingConfig().logRoleArn()).isEqualTo(loggingConfig.getLogRoleArn());
        assertThat(request.schemaHandlerPackage()).isEqualTo(model.getSchemaHandlerPackage());
        assertThat(request.typeAsString()).isEqualTo(TYPE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToDeleteRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToDeleteRequest(null, logger))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToDeleteRequest_defaultVersion() {
        ResourceModel model = ResourceModel.builder()
                .isDefaultVersion(true)
                .typeName(TYPE_NAME)
                .build();
        DeregisterTypeRequest request = Translator.translateToDeleteRequest(model, logger);

        assertThat(request.arn()).isNull();
        assertThat(request.typeAsString()).isEqualTo(TYPE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToCreateRequest_notDefaultVersion() {
        ResourceModel model = ResourceModel.builder()
                .isDefaultVersion(false)
                .arn(TYPE_VERSION_ARN_00000001)
                .build();
        DeregisterTypeRequest request = Translator.translateToDeleteRequest(model, logger);

        assertThat(request.arn()).isEqualTo(model.getArn());
        assertThat(request.type()).isNull();
        assertThat(request.typeName()).isNull();
    }

    @Test
    public void translateToCreateRequest_notDefaultVersion_nologger() {
        ResourceModel model = ResourceModel.builder()
                .isDefaultVersion(false)
                .arn(TYPE_VERSION_ARN_00000001)
                .build();
        assertThatThrownBy(() -> Translator.translateToDeleteRequest(model, null))
                .hasNoCause()
                .hasMessageStartingWith("logger is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateFromReadResponse_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateFromReadResponse(null))
                .hasNoCause()
                .hasMessageStartingWith("awsResponse is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateFromReadResponse_noTimestamps() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000002)
                .description(DESCRIPTION)
                .documentationUrl(DOCUMENTATION_URL)
                .executionRoleArn(EXECUTION_ROLE_ARN)
                .provisioningType(ProvisioningType.FULLY_MUTABLE)
                .schema(SCHEMA)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(Visibility.PRIVATE)
                .defaultVersionId(VERSION_ID)
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);

        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getLoggingConfig()).isNull();
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());
    }

    @Test
    public void translateFromReadResponse_allTimestamps() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000002)
                .description(DESCRIPTION)
                .documentationUrl(DOCUMENTATION_URL)
                .executionRoleArn(EXECUTION_ROLE_ARN)
                .provisioningType(ProvisioningType.FULLY_MUTABLE)
                .schema(SCHEMA)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(Visibility.PRIVATE)
                .defaultVersionId(VERSION_ID)
                .lastUpdated(Instant.now())
                .timeCreated(Instant.now())
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);

        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getLoggingConfig()).isNull();
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());
    }

    @Test
    public void translateFromReadResponse_withLoggingConfig() {
        software.amazon.awssdk.services.cloudformation.model.LoggingConfig loggingConfig =
                software.amazon.awssdk.services.cloudformation.model.LoggingConfig.builder()
                        .logGroupName(LOG_GROUP_NAME)
                        .logRoleArn(LOG_ROLE_ARN)
                        .build();

        DescribeTypeResponse response = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000002)
                .description(DESCRIPTION)
                .documentationUrl(DOCUMENTATION_URL)
                .executionRoleArn(EXECUTION_ROLE_ARN)
                .loggingConfig(loggingConfig)
                .provisioningType(ProvisioningType.FULLY_MUTABLE)
                .schema(SCHEMA)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(Visibility.PRIVATE)
                .defaultVersionId(VERSION_ID)
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);

        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());
        assertThat(model.getLoggingConfig().getLogGroupName()).isEqualTo(loggingConfig.logGroupName());
        assertThat(model.getLoggingConfig().getLogRoleArn()).isEqualTo(loggingConfig.logRoleArn());
    }

    @Test
    public void translateFromListResponse_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateFromListResponse(null))
                .hasNoCause()
                .hasMessageStartingWith("awsResponse is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_noModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null, logger))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_noLogger() {
        ResourceModel model = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000002)
                .build();
        assertThatThrownBy(() -> Translator.translateToReadRequest(model, null))
                .hasNoCause()
                .hasMessageStartingWith("logger is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateFromListResponse_noTypes() {
        ListTypeVersionsResponse response = ListTypeVersionsResponse.builder().build();

        List<ResourceModel> model = Translator.translateFromListResponse(response);

        assertThat(model.isEmpty()).isTrue();
    }

    @Test
    public void translateFromListResponse_withTypes() {
        List<TypeVersionSummary> typeSummaries = new ArrayList<>();
        typeSummaries.add(TypeVersionSummary.builder().arn(TYPE_VERSION_ARN_00000001).build());
        typeSummaries.add(TypeVersionSummary.builder().arn(TYPE_VERSION_ARN_00000002).build());

        ListTypeVersionsResponse response = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeSummaries)
                .build();

        List<ResourceModel> model = Translator.translateFromListResponse(response);

        assertThat(model.size()).isEqualTo(2);
        assertThat(model.get(0).getArn()).isEqualTo(TYPE_VERSION_ARN_00000001);
        assertThat(model.get(1).getArn()).isEqualTo(TYPE_VERSION_ARN_00000002);
    }
}
