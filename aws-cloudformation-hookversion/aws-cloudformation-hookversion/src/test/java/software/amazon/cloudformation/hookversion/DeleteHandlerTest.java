package software.amazon.cloudformation.hookversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractMockTestBase<CloudFormationClient> {

    private static final String AWS_PARTITION = "aws";
    private static final String REGION = "us-west-2";
    private static final String AWS_ACCOUNT_ID = "123456789012";
    private static final String TYPE_NAME = "AWS::Demo::Hook";
    private static final String VISIBILITY = "PRIVATE";
    private static final String TYPE_VERSION_ARN_00000001 = "arn:aws:cloudformation:us-west-2:123456789012:type/hook/AWS-Demo-Hook/00000001";
    private static final String RESOURCE_REPO_GIT = "https://github.com/myorg/hook/repo.git";
    private static final String VERSION_ID = "00000001";
    private static final String DEPRECATED_STATUS = "LIVE";
    private static final String TYPE = "HOOK";

    private final DeleteHandler handler = new DeleteHandler();

    protected DeleteHandlerTest() {
        super(CloudFormationClient.class);
    }

    @Test
    public void handleRequest_Success() {
        final CloudFormationClient client = getServiceClient();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus(DEPRECATED_STATUS)
                .isDefaultVersion(false)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final DeregisterTypeResponse deregisterTypeResponse = DeregisterTypeResponse.builder().build();
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenReturn(deregisterTypeResponse);

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .awsPartition(AWS_PARTITION)
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_GeneralError() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus(DEPRECATED_STATUS)
                .isDefaultVersion(false)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        // throw on DeregisterType
        final CfnNotFoundException exception = new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceModel.getPrimaryIdentifier().toString());
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(exception);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + resourceModel.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_NotFoundAfterRead() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus(DEPRECATED_STATUS)
                .isDefaultVersion(false)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        // type deregistered out of band
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + resourceModel.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }


    @Test
    public void handleRequest_CantDeleteDefaultVersion() {
        final CloudFormationClient client = getServiceClient();

        final ResourceModel resourceModel = ResourceModel.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .typeName(TYPE_NAME)
                .build();

        final DescribeTypeResponse describeTypeResponse = DescribeTypeResponse.builder()
                .arn(TYPE_VERSION_ARN_00000001)
                .defaultVersionId(VERSION_ID)
                .deprecatedStatus(DEPRECATED_STATUS)
                .isDefaultVersion(true)
                .sourceUrl(RESOURCE_REPO_GIT)
                .type(TYPE)
                .typeName(TYPE_NAME)
                .visibility(VISIBILITY)
                .build();
        when(client.describeType(ArgumentMatchers.any(DescribeTypeRequest.class)))
                .thenReturn(describeTypeResponse);

        final CfnNotFoundException exception = new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceModel.getPrimaryIdentifier().toString());
        when(client.deregisterType(ArgumentMatchers.any(DeregisterTypeRequest.class)))
                .thenThrow(exception);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(resourceModel)
                .build();

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasNoCause()
                .hasMessage("Resource of type '" + ResourceModel.TYPE_NAME + "' with identifier '" + resourceModel.getPrimaryIdentifier().toString() + "' was not found.")
                .isExactlyInstanceOf(CfnNotFoundException.class);
    }
}
