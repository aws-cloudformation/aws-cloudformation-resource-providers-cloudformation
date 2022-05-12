package software.amazon.cloudformation.hookdefaultversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.Visibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    private static final String TYPE_NAME = "AWS::Test::Resource";
    private static final String VERSION_ID = "00000002";
    private static final String TYPE_VERSION_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource/00000002";
    private static final String ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/resource/AWS-Test-Resource";
    private static final String TYPE = "HOOK";

    @Test
    public void translateToUpdateRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToUpdateRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToUpdateRequest_typeAndVersion() {
        ResourceModel model = ResourceModel.builder()
                .typeName(TYPE_NAME)
                .versionId(VERSION_ID)
                .build();
        SetTypeDefaultVersionRequest request = Translator.translateToUpdateRequest(model);

        assertThat(request.arn()).isNull();
        assertThat(request.typeName()).isEqualTo(TYPE_NAME);
        assertThat(request.versionId()).isEqualTo(VERSION_ID);
    }

    @Test
    public void translateToUpdateRequest_arn() {
        ResourceModel model = ResourceModel.builder()
                .typeVersionArn(TYPE_VERSION_ARN)
                .versionId(VERSION_ID)
                .build();
        SetTypeDefaultVersionRequest request = Translator.translateToUpdateRequest(model);

        assertThat(request.arn()).isEqualTo(TYPE_VERSION_ARN);
        assertThat(request.type()).isNull();
        assertThat(request.typeName()).isNull();
        assertThat(request.versionId()).isNull();
    }

    @Test
    public void translateToReadRequest_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_typeAndVersion() {
        ResourceModel model = ResourceModel.builder()
                .arn(ARN)
                .build();
        DescribeTypeRequest request = Translator.translateToReadRequest(model);

        assertThat(request.arn()).isEqualTo(ARN);
    }

    @Test
    public void translateForRead_nullResourceModel() {
        assertThatThrownBy(() -> Translator.translateFromReadResponse(null))
                .hasNoCause()
                .hasMessageStartingWith("awsResponse is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateForRead() {
        DescribeTypeResponse response = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN)
                .defaultVersionId(VERSION_ID)
                .description("some resource")
                .documentationUrl("https://mydocs.org/some-resource")
                .executionRoleArn("arn:aws:iam::123456789012:role/AppRole")
                .schema("{ schema }")
                .sourceUrl("https://github.com/some-resource")
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(Visibility.PRIVATE)
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);

        assertThat(model.getTypeVersionArn()).isEqualTo(response.arn());
        assertThat(model.getVersionId()).isEqualTo(response.defaultVersionId());
        assertThat(model.getTypeName()).isEqualTo(response.typeName());
    }
}
