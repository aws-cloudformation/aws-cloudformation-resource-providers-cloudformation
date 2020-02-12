package software.amazon.cloudformation.resourceversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.ProvisioningType;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeSummary;
import software.amazon.awssdk.services.cloudformation.model.Visibility;
import software.amazon.cloudformation.proxy.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    @Mock
    private Logger logger;

    @Test
    public void translateToCreateRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToCreateRequest(null))
            .hasNoCause()
            .hasMessage("model is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToCreateRequest_nullLoggingConfig() {
        ResourceModel model = ResourceModel.builder()
            .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
            .schemaHandlerPackage("example-bucket/some/path/code.zip")
            .typeName("AWS::Test::Resource")
            .build();
        RegisterTypeRequest request = Translator.translateToCreateRequest(model);

        assertThat(request.executionRoleArn()).isEqualTo(model.getExecutionRoleArn());
        assertThat(request.loggingConfig()).isNull();
        assertThat(request.schemaHandlerPackage()).isEqualTo(model.getSchemaHandlerPackage());
        assertThat(request.type()).isEqualTo(RegistryType.RESOURCE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToCreateRequest_withLoggingConfig() {
        LoggingConfig loggingConfig = LoggingConfig.builder()
            .logGroupName("my-group")
            .logRoleArn("arn:aws:iam::123456789012:role/LoggingRole")
            .build();

        ResourceModel model = ResourceModel.builder()
            .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
            .loggingConfig(loggingConfig)
            .schemaHandlerPackage("example-bucket/some/path/code.zip")
            .typeName("AWS::Test::Resource")
            .build();
        RegisterTypeRequest request = Translator.translateToCreateRequest(model);

        assertThat(request.executionRoleArn()).isEqualTo(model.getExecutionRoleArn());
        assertThat(request.loggingConfig().logGroupName()).isEqualTo(loggingConfig.getLogGroupName());
        assertThat(request.loggingConfig().logRoleArn()).isEqualTo(loggingConfig.getLogRoleArn());
        assertThat(request.schemaHandlerPackage()).isEqualTo(model.getSchemaHandlerPackage());
        assertThat(request.type()).isEqualTo(RegistryType.RESOURCE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToDeleteRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToDeleteRequest(null, logger))
            .hasNoCause()
            .hasMessage("model is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToDeleteRequest_defaultVersion() {
        ResourceModel model = ResourceModel.builder()
            .isDefaultVersion(true)
            .typeName("AWS::Test::Resource")
            .build();
        DeregisterTypeRequest request = Translator.translateToDeleteRequest(model, logger);

        assertThat(request.arn()).isNull();
        assertThat(request.type()).isEqualTo(RegistryType.RESOURCE);
        assertThat(request.typeName()).isEqualTo(model.getTypeName());
    }

    @Test
    public void translateToCreateRequest_notDefaultVersion() {
        ResourceModel model = ResourceModel.builder()
            .isDefaultVersion(false)
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource")
            .build();
        DeregisterTypeRequest request = Translator.translateToDeleteRequest(model, logger);

        assertThat(request.arn()).isEqualTo(model.getArn());
        assertThat(request.type()).isNull();
        assertThat(request.typeName()).isNull();
    }

    @Test
    public void translateForRead_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateForRead(null))
            .hasNoCause()
            .hasMessage("response is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateForRead_noTimestamps() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
            .description("some resource")
            .documentationUrl("https://mydocs.org/some-resource")
            .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
            .provisioningType(ProvisioningType.FULLY_MUTABLE)
            .schema("{ schema }")
            .sourceUrl("https://github.com/some-resource")
            .type(RegistryType.RESOURCE)
            .typeName("AWS::Test::Resource")
            .visibility(Visibility.PRIVATE)
            .defaultVersionId("1")
            .build();

        ResourceModel model = Translator.translateForRead(response);

//        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getDescription()).isEqualTo(response.description());
        assertThat(model.getDocumentationUrl()).isEqualTo(response.documentationUrl());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getLoggingConfig()).isNull();
        assertThat(model.getProvisioningType()).isEqualTo(response.provisioningTypeAsString());
        assertThat(model.getSchema()).isEqualTo(response.schema());
        assertThat(model.getSourceUrl()).isEqualTo(response.sourceUrl());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());

        assertThat(model.getLastUpdated()).isNull();
        assertThat(model.getTimeCreated()).isNull();
    }

    @Test
    public void translateForRead_allTimestamps() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
            .description("some resource")
            .documentationUrl("https://mydocs.org/some-resource")
            .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
            .provisioningType(ProvisioningType.FULLY_MUTABLE)
            .schema("{ schema }")
            .sourceUrl("https://github.com/some-resource")
            .type(RegistryType.RESOURCE)
            .typeName("AWS::Test::Resource")
            .visibility(Visibility.PRIVATE)
            .defaultVersionId("1")
            .lastUpdated(Instant.now())
            .timeCreated(Instant.now())
            .build();

        ResourceModel model = Translator.translateForRead(response);

//        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getDescription()).isEqualTo(response.description());
        assertThat(model.getDocumentationUrl()).isEqualTo(response.documentationUrl());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getLoggingConfig()).isNull();
        assertThat(model.getProvisioningType()).isEqualTo(response.provisioningTypeAsString());
        assertThat(model.getSchema()).isEqualTo(response.schema());
        assertThat(model.getSourceUrl()).isEqualTo(response.sourceUrl());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());

        assertThat(model.getLastUpdated()).isEqualTo(response.lastUpdated().toString());
        assertThat(model.getTimeCreated()).isEqualTo(response.timeCreated().toString());
    }

    @Test
    public void translateForRead_withLoggingConfig() {
        software.amazon.awssdk.services.cloudformation.model.LoggingConfig loggingConfig =
            software.amazon.awssdk.services.cloudformation.model.LoggingConfig.builder()
                .logGroupName("my-group")
                .logRoleArn("arn:aws:iam::123456789012:role/LoggingRole")
                .build();

        DescribeTypeResponse response = DescribeTypeResponse.builder()
            .description("some resource")
            .documentationUrl("https://mydocs.org/some-resource")
            .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
            .loggingConfig(loggingConfig)
            .provisioningType(ProvisioningType.FULLY_MUTABLE)
            .schema("{ schema }")
            .sourceUrl("https://github.com/some-resource")
            .type(RegistryType.RESOURCE)
            .typeName("AWS::Test::Resource")
            .visibility(Visibility.PRIVATE)
            .defaultVersionId("1")
            .build();

        ResourceModel model = Translator.translateForRead(response);

//        assertThat(model.getIsDefaultVersion()).isEqualTo(response.isDefaultVersion());
        assertThat(model.getDescription()).isEqualTo(response.description());
        assertThat(model.getDocumentationUrl()).isEqualTo(response.documentationUrl());
        assertThat(model.getExecutionRoleArn()).isEqualTo(response.executionRoleArn());
        assertThat(model.getProvisioningType()).isEqualTo(response.provisioningTypeAsString());
        assertThat(model.getSchema()).isEqualTo(response.schema());
        assertThat(model.getSourceUrl()).isEqualTo(response.sourceUrl());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
        assertThat(model.getVisibility()).isEqualTo(response.visibilityAsString());

        assertThat(model.getLastUpdated()).isNull();
        assertThat(model.getTimeCreated()).isNull();

        assertThat(model.getLoggingConfig().getLogGroupName()).isEqualTo(loggingConfig.logGroupName());
        assertThat(model.getLoggingConfig().getLogRoleArn()).isEqualTo(loggingConfig.logRoleArn());
    }


    @Test
    public void translateForList_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateForList(null))
            .hasNoCause()
            .hasMessage("response is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateForList_noTypes() {
        ListTypesResponse response = ListTypesResponse.builder().build();

        List<ResourceModel> model = Translator.translateForList(response);

        assertThat(model.isEmpty()).isTrue();
    }


    @Test
    public void translateForList_withTypes() {
        List<TypeSummary> typeSummaries = new ArrayList<>();
        typeSummaries.add(TypeSummary.builder().typeArn("Type1").build());
        typeSummaries.add(TypeSummary.builder().typeArn("Type2").build());

        ListTypesResponse response = ListTypesResponse.builder()
            .typeSummaries(typeSummaries)
            .build();

        List<ResourceModel> model = Translator.translateForList(response);

        assertThat(model.size()).isEqualTo(2);
        assertThat(model.get(0).getArn()).isEqualTo("Type1");
        assertThat(model.get(1).getArn()).isEqualTo("Type2");
    }
}
