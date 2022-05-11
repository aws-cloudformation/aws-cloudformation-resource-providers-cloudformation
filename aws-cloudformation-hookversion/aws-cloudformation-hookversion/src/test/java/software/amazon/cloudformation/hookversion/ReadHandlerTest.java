package software.amazon.cloudformation.hookversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String VISIBILITY = "PRIVATE";
    private static final String TYPE = "HOOK";
    private static final String TYPE_VERSION_ARN_00000001 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String TYPE_ARN = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook";
    private static final String RESOURCE_REPO_GIT = "https://github.com/myorg/hook/repo.git";
    private static final String VERSION_ID = "00000001";

    private final ReadHandler handler = new ReadHandler();

    protected ReadHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CloudFormationClient client = getServiceClient();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus("LIVE")
                .isDefaultVersion(true)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceModel inModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .build();

        final ResourceModel outModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .isDefaultVersion(true)
                .typeArn(TYPE_ARN)
                .typeName(TYPE_NAME)
                .versionId(VERSION_ID)
                .visibility(VISIBILITY)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(inModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(outModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeprecatedTypeDoesNotExist() {
        final CloudFormationClient client = getServiceClient();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus("DEPRECATED")
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final ResourceModel model = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, null, loggerProxy));
    }

    @Test
    public void handleRequest_GeneralError() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();

        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenThrow(CfnRegistryException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCauseExactlyInstanceOf(CfnRegistryException.class)
                .hasMessage(null)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_NotFound() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();

        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type 'AWS::CloudFormation::HookVersion' with identifier '{\"/properties/Arn\":\"" + TYPE_VERSION_ARN_00000001 + "\"}' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_BadInput_NoModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource Model can not be null")
                .isExactlyInstanceOf(NullPointerException.class);
    }
}
