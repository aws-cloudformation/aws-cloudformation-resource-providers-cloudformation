package software.amazon.cloudformation.moduleversion;

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
    private final String modulePackage = "s3://test-module-package/";

    @Test
    public void translateToCreateRequest_NullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToCreateRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToCreateRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .modulePackage(modulePackage)
                .moduleName(moduleName)
                .build();

        final RegisterTypeRequest registerTypeRequest = Translator.translateToCreateRequest(model);

        assertThat(registerTypeRequest.schemaHandlerPackage()).isEqualTo(model.getModulePackage());
        assertThat(registerTypeRequest.typeAsString()).isEqualTo("MODULE");
        assertThat(registerTypeRequest.typeName()).isEqualTo(model.getModuleName());
        assertThat(registerTypeRequest.clientRequestToken()).isNotEmpty();
    }

    @Test
    public void translateToDescribeTypeRegistrationRequest_NullRegistrationToken() {
        assertThatThrownBy(() -> Translator.translateToDescribeTypeRegistrationRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("registrationToken is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToDescribeTypeRegistrationRequest_Success() {
        final String registrationToken = "dummy_token";
        final DescribeTypeRegistrationRequest describeTypeRegistrationRequest = Translator.translateToDescribeTypeRegistrationRequest(registrationToken);

        assertThat(describeTypeRegistrationRequest.registrationToken()).isEqualTo(registrationToken);
    }

    @Test
    public void translateToReadRequest_NullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToReadRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
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
                .type("MODULE")
                .typeName(moduleName)
                .build();

        final ResourceModel model = Translator.translateFromReadResponse(describeTypeResponse);

        assertThat(model.getArn()).isEqualTo(describeTypeResponse.arn());
        assertThat(model.getModuleName()).isEqualTo(describeTypeResponse.typeName());
    }

    @Test
    public void translateToDeleteRequest_NullResourceModel() {
        assertThatThrownBy(() -> Translator.translateToDeleteRequest(null))
                .hasNoCause()
                .hasMessageStartingWith("model is marked")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void translateToDeleteRequest_IsDefaultVersion_Success() {
        final ResourceModel model = ResourceModel.builder()
                .isDefaultVersion(true)
                .moduleName(moduleName)
                .build();

        final DeregisterTypeRequest deregisterTypeRequest = Translator.translateToDeleteRequest(model);

        assertThat(deregisterTypeRequest.typeAsString()).isEqualTo("MODULE");
        assertThat(deregisterTypeRequest.typeName()).isEqualTo(model.getModuleName());
    }

    @Test
    public void translateToDeleteRequest_IsNotDefaultVersion_Success() {
        final ResourceModel model = ResourceModel.builder()
                .arn(arn)
                .isDefaultVersion(false)
                .build();

        final DeregisterTypeRequest deregisterTypeRequest = Translator.translateToDeleteRequest(model);

        assertThat(deregisterTypeRequest.arn()).isEqualTo(model.getArn());
    }

    @Test
    public void translateToListTypesRequest_Success() {
        final String nextToken = "dummy_next_token";

        final ListTypesRequest listTypesRequest = Translator.translateToListTypesRequest(nextToken);

        assertThat(listTypesRequest.maxResults()).isEqualTo(100);
        assertThat(listTypesRequest.nextToken()).isEqualTo(nextToken);
    }

    @Test
    public void translateFromListTypesResponse_Success() {
        final String resourceName = "My::Test::Resource";
        final String moduleName1 = resourceName + "1::MODULE";
        final String moduleName2 = resourceName + "2::MODULE";
        final String moduleName3 = resourceName + "3::MODULE";
        final TypeSummary typeSummary1 = TypeSummary.builder().type("MODULE").typeName(moduleName1).build();
        final TypeSummary typeSummary2 = TypeSummary.builder().type("RESOURCE").typeName(resourceName).build();
        final TypeSummary typeSummary3 = TypeSummary.builder().type("MODULE").typeName(moduleName2).build();
        final TypeSummary typeSummary4 = TypeSummary.builder().type("MODULE").typeName(moduleName3).build();
        final List<TypeSummary> typeSummaries = Arrays.asList(typeSummary1, typeSummary2, typeSummary3, typeSummary4);
        final ListTypesResponse listTypesResponse = ListTypesResponse.builder().typeSummaries(typeSummaries).build();

        final List<ResourceModel> models = Translator.translateFromListTypesResponse(listTypesResponse);

        assertThat(models.size()).isEqualTo(3);
        assertThat(models.stream().map(ResourceModel::getModuleName).collect(Collectors.toList()))
                .contains(moduleName1, moduleName2, moduleName3);
    }

    @Test
    public void translateToListTypeVersionsRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();
        final DeprecatedStatus deprecatedStatus = DeprecatedStatus.LIVE;
        final String nextToken = "dummy_next_token";


        final ListTypeVersionsRequest listTypeVersionsRequest = Translator.translateToListTypeVersionsRequest(model, nextToken, deprecatedStatus);

        assertThat(listTypeVersionsRequest.deprecatedStatus()).isEqualTo(deprecatedStatus);
        assertThat(listTypeVersionsRequest.maxResults()).isEqualTo(100);
        assertThat(listTypeVersionsRequest.nextToken()).isEqualTo(nextToken);
        assertThat(listTypeVersionsRequest.typeAsString()).isEqualTo("MODULE");
        assertThat(listTypeVersionsRequest.typeName()).isEqualTo(model.getModuleName());
    }

    @Test
    public void translateFromListTypeVersionsResponse_Success() {
        final String moduleArn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE";
        final String arn1 = moduleArn + "/00000001";
        final String arn2 = moduleArn + "/00000002";
        final String arn3 = moduleArn + "/00000003";
        final TypeVersionSummary typeVersionSummary1 = TypeVersionSummary.builder().arn(arn1).build();
        final TypeVersionSummary typeVersionSummary2 = TypeVersionSummary.builder().arn(arn2).build();
        final TypeVersionSummary typeVersionSummary3 = TypeVersionSummary.builder().arn(arn3).build();
        final List<TypeVersionSummary> typeVersionSummaries = Arrays.asList(typeVersionSummary1, typeVersionSummary2, typeVersionSummary3);
        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder().typeVersionSummaries(typeVersionSummaries).build();

        final List<ResourceModel> models = Translator.translateFromListTypeVersionsResponse(listTypeVersionsResponse);

        assertThat(models.size()).isEqualTo(3);
        assertThat(models.stream().map(ResourceModel::getArn).collect(Collectors.toList()))
                .contains(arn1, arn2, arn3);
    }
}
