package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.InvalidOperationException;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.Validator;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_RUNNING_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_TARGETS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_TARGETS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.STACK_SET_CONFIGS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.BASE_CALLBACK_DELAY_SECONDS;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    private UpdateHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private Validator validator;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        validator = mock(Validator.class);
        handler = new UpdateHandler(validator);
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATED_MODEL)
                .previousResourceState(SIMPLE_MODEL)
                .build();
    }

    @Test
    public void handleRequest_NotUpdatable_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_MODEL)
                .previousResourceState(SIMPLE_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AllUpdatesStabilized_Success() {

        final Map<UpdateOperations, Boolean> updateOperationsMap = new EnumMap<>(UpdateOperations.class);
        updateOperationsMap.put(STACK_SET_CONFIGS, true);
        updateOperationsMap.put(DELETE_INSTANCES_BY_REGIONS, true);
        updateOperationsMap.put(DELETE_INSTANCES_BY_TARGETS, true);
        updateOperationsMap.put(ADD_INSTANCES_BY_REGIONS, true);
        updateOperationsMap.put(ADD_INSTANCES_BY_TARGETS, true);

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByTargetsStarted(true)
                .deleteStacksByRegionsStarted(true)
                .addStacksByRegionsStarted(true)
                .addStacksByTargetsStarted(true)
                .operationId(OPERATION_ID_1)
                .operationsStabilizationMap(updateOperationsMap)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStackSetNotStarted_InProgress() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doReturn(UPDATE_STACK_SET_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStackSetNotStabilized_InProgress() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
                .elapsedTime(BASE_CALLBACK_DELAY_SECONDS)
                .build();


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStacksRegionsNotStarted_InProgress() {

        doReturn(DELETE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SelfManaged_DeleteStacksRegionsNotStarted_InProgress() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATED_SELF_MANAGED_MODEL)
                .previousResourceState(SELF_MANAGED_MODEL)
                .build();

        doReturn(DELETE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeleteStacksTargetsNotStarted_InProgress() {

        doReturn(DELETE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .deleteStacksByTargetsStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddStacksRegionsNotStarted_InProgress() {

        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .deleteStacksByTargetsStarted(true)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .deleteStacksByTargetsStarted(true)
                .addStacksByRegionsStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddStacksTargetsNotStarted_InProgress() {

        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .deleteStacksByTargetsStarted(true)
                .addStacksByRegionsStarted(true)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);
        inputContext.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_REGIONS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksByRegionsStarted(true)
                .deleteStacksByTargetsStarted(true)
                .addStacksByRegionsStarted(true)
                .addStacksByTargetsStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);
        outputContext.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_REGIONS, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handlerRequest_InvalidOperationException() {

        doThrow(InvalidOperationException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_StackSetNotFoundException() {

        doThrow(StackSetNotFoundException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_OperationInProgressException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doThrow(OperationInProgressException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext outputContext = CallbackContext.builder()
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .retries(1)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(outputContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(outputContext.getCurrentDelaySeconds());
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
