package software.amazon.cloudformation.resourceversionalias;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ProvisioningType;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.Visibility;
import software.amazon.cloudformation.proxy.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    @Mock
    private Logger logger;

    @Test
    public void translateToUpdateRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToUpdateRequest(null, logger))
            .hasNoCause()
            .hasMessage("model is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToUpdateRequest_typeAndVersion() {
        ResourceModel model = ResourceModel.builder()
            .typeName("AWS::Test::Resource")
            .defaultVersionId("00000002")
            .build();
        SetTypeDefaultVersionRequest request = Translator.translateToUpdateRequest(model, logger);

        assertThat(request.arn()).isNull();
        assertThat(request.type()).isEqualTo(RegistryType.RESOURCE);
        assertThat(request.typeName()).isEqualTo("AWS::Test::Resource");
        assertThat(request.versionId()).isEqualTo("00000002");
    }

    @Test
    public void translateToUpdateRequest_arn() {
        ResourceModel model = ResourceModel.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource/00000002")
            .defaultVersionId("00000002")
            .build();
        SetTypeDefaultVersionRequest request = Translator.translateToUpdateRequest(model, logger);

        assertThat(request.arn()).isEqualTo("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource/00000002");
        assertThat(request.type()).isNull();
        assertThat(request.typeName()).isNull();
        assertThat(request.versionId()).isNull();
    }

    @Test
    public void translateToReadRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null))
            .hasNoCause()
            .hasMessage("model is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_typeAndVersion() {
        ResourceModel model = ResourceModel.builder()
            .typeName("AWS::Test::Resource")
            .defaultVersionId("00000002")
            .build();
        DescribeTypeRequest request = Translator.translateToReadRequest(model);

        assertThat(request.arn()).isNull();
        assertThat(request.type()).isEqualTo(RegistryType.RESOURCE);
        assertThat(request.typeName()).isEqualTo("AWS::Test::Resource");
        assertThat(request.versionId()).isEqualTo("00000002");
    }

    @Test
    public void translateForRead_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateForRead(null))
            .hasNoCause()
            .hasMessage("response is marked @NonNull but is null")
            .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateForRead() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
            .arn("arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource/00000002")
            .defaultVersionId("00000003")
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

        assertThat(model.getArn()).isEqualTo(response.arn());
        assertThat(model.getDefaultVersionId()).isEqualTo(response.defaultVersionId());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
    }
}
