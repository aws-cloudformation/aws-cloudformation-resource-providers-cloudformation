package software.amazon.cloudformation.moduleversion;

import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeSummary;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractMockTestBase<CloudFormationClient> {
    private ListHandler handler = new ListHandler();
    private CloudFormationClient client = getServiceClient();

    private final String arnBase = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE";
    private final String moduleNameBase  = "My::Test::Resource::MODULE";

    private final TypeSummary typeSummary1 = TypeSummary.builder().defaultVersionId("00000001").type("MODULE").typeArn(arnBase + 1).typeName(moduleNameBase + 1).build();
    private final TypeSummary typeSummary2 = TypeSummary.builder().defaultVersionId("00000010").type("MODULE").typeArn(arnBase + 2).typeName(moduleNameBase + 2).build();
    private final TypeSummary typeSummary3 = TypeSummary.builder().defaultVersionId("00000100").type("MODULE").typeArn(arnBase + 3).typeName(moduleNameBase + 3).build();
    private final TypeSummary typeSummary4 = TypeSummary.builder().defaultVersionId("00001000").type("MODULE").typeArn(arnBase + 4).typeName(moduleNameBase + 4).build();
    private final TypeSummary typeSummary5 = TypeSummary.builder().defaultVersionId("00010000").type("MODULE").typeArn(arnBase + 5).typeName(moduleNameBase + 5).build();
    private final List<TypeSummary> typeSummaries = Arrays.asList(typeSummary1, typeSummary2, typeSummary3, typeSummary4, typeSummary5);

    private final TypeVersionSummary typeVersionSummary1 = TypeVersionSummary.builder().versionId("00000001").type("MODULE").arn(arnBase + "/00000001").typeName(moduleNameBase).build();
    private final TypeVersionSummary typeVersionSummary2 = TypeVersionSummary.builder().versionId("00000002").type("MODULE").arn(arnBase + "/00000002").typeName(moduleNameBase).build();
    private final TypeVersionSummary typeVersionSummary3 = TypeVersionSummary.builder().versionId("00000003").type("MODULE").arn(arnBase + "/00000003").typeName(moduleNameBase).build();
    private final TypeVersionSummary typeVersionSummary4 = TypeVersionSummary.builder().versionId("00000004").type("MODULE").arn(arnBase + "/00000004").typeName(moduleNameBase).build();
    private final TypeVersionSummary typeVersionSummary5 = TypeVersionSummary.builder().versionId("00000005").type("MODULE").arn(arnBase + "/00000005").typeName(moduleNameBase).build();
    private final List<TypeVersionSummary> typeVersionSummaries = Arrays.asList(typeVersionSummary1, typeVersionSummary2, typeVersionSummary3, typeVersionSummary4, typeVersionSummary5);

    private final ResourceModel modelWithName1 = ResourceModel.builder().moduleName(moduleNameBase + 1).build();
    private final ResourceModel modelWithName2 = ResourceModel.builder().moduleName(moduleNameBase + 2).build();
    private final ResourceModel modelWithName3 = ResourceModel.builder().moduleName(moduleNameBase + 3).build();
    private final ResourceModel modelWithName4 = ResourceModel.builder().moduleName(moduleNameBase + 4).build();
    private final ResourceModel modelWithName5 = ResourceModel.builder().moduleName(moduleNameBase + 5).build();
    private final List<ResourceModel> modelsWithName = Arrays.asList(modelWithName2, modelWithName3, modelWithName4, modelWithName5);

    private final ResourceModel modelWithArn1 = ResourceModel.builder().arn(arnBase + "/00000001").build();
    private final ResourceModel modelWithArn2 = ResourceModel.builder().arn(arnBase + "/00000002").build();
    private final ResourceModel modelWithArn3 = ResourceModel.builder().arn(arnBase + "/00000003").build();
    private final ResourceModel modelWithArn4 = ResourceModel.builder().arn(arnBase + "/00000004").build();
    private final ResourceModel modelWithArn5 = ResourceModel.builder().arn(arnBase + "/00000005").build();
    private final List<ResourceModel> modelsWithArn = Arrays.asList(modelWithArn1, modelWithArn2, modelWithArn3, modelWithArn4, modelWithArn5);

    private final String nextToken = "test next token";

    protected ListHandlerTest() {
        super(CloudFormationClient.class);
    }

    @BeforeEach
    public void setup() {
        when(this.client.serviceName()).thenReturn("cloudformation");
    }

    @Test
    public void handleRequest_EmptyTokenSetToNull() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("")
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .nextToken(nextToken)
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        ArgumentCaptor<ListTypesRequest> captor = ArgumentCaptor.forClass(ListTypesRequest.class);
        verify(client, times(1)).listTypes(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().nextToken()).isNull();
    }

    @Test
    public void handleRequest_ListsTypesWhenNoModuleSpecified() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder().build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        verify(client, times(0)).listTypeVersions(any(ListTypeVersionsRequest.class));
    }

    @Test
    public void handleRequest_ListsTypeVersionsWhenModuleSpecified_InRequest() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder().build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        handler.handleRequest(proxy, request, null, loggerProxy);

        verify(client, times(0)).listTypes(any(ListTypesRequest.class));
        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
    }

    @Test
    public void handleRequest_ListsTypeVersionsWhenModuleSpecified_InCallbackContext() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder().build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.setModuleToList(ResourceModel.builder().moduleName(moduleNameBase).build());
        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(0)).listTypes(any(ListTypesRequest.class));
        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
    }

    @Test
    public void handleRequest_ListTypes_BasicSuccess() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .nextToken(nextToken)
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEmpty();
        assertThat(response.getNextToken()).isEqualTo("test next token");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListTypes_RegistryError() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final CfnRegistryException exception = CfnRegistryException.builder().build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(exception)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_ListTypes_ReturnsOnlyModules() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final LinkedList<TypeSummary> summariesWithNonModules = new LinkedList<>();
        summariesWithNonModules.add(TypeSummary.builder()
                .defaultVersionId("00000001")
                .type(RegistryType.RESOURCE)
                .typeArn("arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource")
                .typeName("My::Test::Resource1")
                .build());
        summariesWithNonModules.addAll(typeSummaries);
        summariesWithNonModules.add(TypeSummary.builder()
                .defaultVersionId("00000001")
                .type(RegistryType.RESOURCE)
                .typeArn("arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource")
                .typeName("My::Test::Resource2")
                .build());
        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(summariesWithNonModules)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final CallbackContext callbackContext = new CallbackContext();
        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        assertThat(
                callbackContext.getModulesToList().stream().map(ResourceModel::getModuleName).collect(Collectors.toList())).isEqualTo(
                modelsWithName.stream().map(ResourceModel::getModuleName).collect(Collectors.toList()));
    }

    @Test
    public void handleRequest_ListTypes_Response_HasEmptyToken() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(response.getNextToken()).isEqualTo("");
    }

    @Test
    public void handleRequest_ListTypes_Response_HasToken() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .nextToken(nextToken)
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(response.getNextToken()).isEqualTo(nextToken);
    }

    @Test
    public void handleRequest_ListTypes_Response_HasCallbackContext() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final CallbackContext callbackContext = new CallbackContext();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext()).isEqualToComparingFieldByField(callbackContext);
    }

    @Test
    public void handleRequest_ListTypes_CallbackContext_ContainsModulesToList() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final CallbackContext callbackContext = new CallbackContext();

        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(callbackContext.getModulesToList()).isNotNull();
        assertThat(callbackContext.getModulesToList()).isNotEmpty();
        assertThat(
                callbackContext.getModulesToList().stream().map(ResourceModel::getModuleName).collect(Collectors.toList())).isEqualTo(
                modelsWithName.stream().map(ResourceModel::getModuleName).collect(Collectors.toList()));
    }

    @Test
    public void handleRequest_ListTypes_CallbackContext_HasModuleToList() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder()
                .typeSummaries(typeSummaries)
                .build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final CallbackContext callbackContext = new CallbackContext();

        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(callbackContext.getModuleToList()).isNotNull();
        assertThat(callbackContext.getModuleToList().getModuleName()).isEqualTo(modelWithName1.getModuleName());
    }

    @Test
    public void handleRequest_ListTypes_CallbackContext_DoesNotHaveModuleToList() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ListTypesResponse listTypesResponse = ListTypesResponse.builder().build();
        when(client.listTypes(any(ListTypesRequest.class)))
                .thenReturn(listTypesResponse);

        final CallbackContext callbackContext = new CallbackContext();

        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypes(any(ListTypesRequest.class));
        assertThat(callbackContext.getModuleToList()).isNull();
    }

    @Test
    public void handleRequest_ListTypeVersions_BasicSuccess() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .nextToken(nextToken)
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotEmpty();
        assertThat(response.getNextToken()).isEqualTo("test next token");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListTypeVersions_RegistryError() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final CfnRegistryException exception = CfnRegistryException.builder().build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, loggerProxy))
                .hasCause(exception)
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_ListTypeVersions_Response_HasEmptyToken() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.addModulesToList(modelsWithName);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(response.getNextToken()).isEqualTo("");
    }

    @Test
    public void handleRequest_ListTypeVersions_Response_HasToken() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .nextToken(nextToken)
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(response.getNextToken()).isEqualTo(nextToken);
    }

    @Test
    public void handleRequest_ListTypeVersions_Response_HasNullToken() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.addModulesToList(new LinkedList<>());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(response.getNextToken()).isNull();
    }

    @Test
    public void handleRequest_ListTypeVersions_Response_HasCallbackContext() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext()).isEqualToComparingFieldByField(callbackContext);
    }

    @Test
    public void handleRequest_ListTypeVersions_Response_HasModuleVersions() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(
                response.getResourceModels().stream().map(ResourceModel::getArn).collect(Collectors.toList())).isEqualTo(
                modelsWithArn.stream().map(ResourceModel::getArn).collect(Collectors.toList()));
    }

    @Test
    public void handleRequest_ListTypeVersions_CallbackContext_HasModuleToList() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ResourceModel.builder().moduleName(moduleNameBase).build())
                .build();

        final ListTypeVersionsResponse listTypeVersionsResponse = ListTypeVersionsResponse.builder()
                .typeVersionSummaries(typeVersionSummaries)
                .build();
        when(client.listTypeVersions(any(ListTypeVersionsRequest.class)))
                .thenReturn(listTypeVersionsResponse);

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.addModulesToList(modelsWithName);

        handler.handleRequest(proxy, request, callbackContext, loggerProxy);

        verify(client, times(1)).listTypeVersions(any(ListTypeVersionsRequest.class));
        assertThat(callbackContext.getModuleToList().getModuleName()).isEqualTo(modelWithName2.getModuleName());
    }
}
