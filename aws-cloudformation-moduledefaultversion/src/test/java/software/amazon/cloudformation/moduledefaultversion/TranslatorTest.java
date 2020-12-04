package software.amazon.cloudformation.moduledefaultversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeSummary;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    private final String arn           = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000021";
    private final String moduleName    = "My::Test::Resource::MODULE";

    @Test
    public void translateToCreateRequest_NullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToCreateRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToCreateRequest_NullArn_Success() {
        final String versionId = "0000003";

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final SetTypeDefaultVersionRequest setTypeDefaultVersionRequest = Translator.translateToCreateRequest(model);

        assertThat(setTypeDefaultVersionRequest.typeAsString()).isEqualTo("MODULE");
        assertThat(setTypeDefaultVersionRequest.typeName()).isEqualTo(model.getModuleName());
        assertThat(setTypeDefaultVersionRequest.versionId()).isEqualTo(model.getVersionId());
    }

    @Test
    public void translateToCreateRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .arn(arn)
                .build();

        final SetTypeDefaultVersionRequest setTypeDefaultVersionRequest = Translator.translateToCreateRequest(model);

        assertThat(setTypeDefaultVersionRequest.arn()).isEqualTo(model.getArn());
    }

    @Test
    public void translateToReadRequest_NullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToReadRequest_NullArn_Success() {
        final String versionId = "0000003";

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .versionId(versionId)
                .build();

        final DescribeTypeRequest describeTypeRequest = Translator.translateToReadRequest(model);

        assertThat(describeTypeRequest.typeAsString()).isEqualTo("MODULE");
        assertThat(describeTypeRequest.typeName()).isEqualTo(model.getModuleName());
        assertThat(describeTypeRequest.versionId()).isEqualTo(model.getVersionId());
    }

    @Test
    public void translateToReadRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .arn(arn)
                .build();

        final DescribeTypeRequest describeTypeRequest = Translator.translateToReadRequest(model);

        assertThat(describeTypeRequest.arn()).isEqualTo(model.getArn());
    }

    @Test
    public void translateFromReadResponse_NullResponse() {
        assertThatThrownBy(() -> Translator.translateFromReadResponse(null))
                .hasNoCause()
                .hasMessageStartingWith("response is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateFromReadResponse_Success() {
        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(arn)
                .build();

        final ResourceModel model = Translator.translateFromReadResponse(describeTypeResponse);

        assertThat(model.getArn()).isEqualTo(describeTypeResponse.arn());
    }
}
