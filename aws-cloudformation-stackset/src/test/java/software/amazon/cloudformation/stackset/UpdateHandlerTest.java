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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.ADD_INSTANCES;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.DELETE_INSTANCES;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.STACK_SET_CONFIGS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.UPDATE_INSTANCES;
import static software.amazon.cloudformation.stackset.util.Stabilizer.BASE_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_SELF_MANAGED;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_SELF_MANAGED;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_RUNNING_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATE_STACK_SET_RESPONSE;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    private UpdateHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new UpdateHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATED_SELF_MANAGED_MODEL)
                .previousResourceState(SELF_MANAGED_MODEL)
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

        final Map<Operations, Boolean> updateOperationsMap = new EnumMap<>(Operations.class);
        updateOperationsMap.put(STACK_SET_CONFIGS, true);
        updateOperationsMap.put(DELETE_INSTANCES, true);
        updateOperationsMap.put(UPDATE_INSTANCES, true);
        updateOperationsMap.put(ADD_INSTANCES, true);

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksStarted(true)
                .addStacksStarted(true)
                .updateStacksStarted(true)
                .templateAnalyzed(true)
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

        doReturn(UPDATE_STACK_SET_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .createStacksQueue(response.getCallbackContext().getCreateStacksQueue())
                .deleteStacksQueue(response.getCallbackContext().getDeleteStacksQueue())
                .updateStacksQueue(response.getCallbackContext().getUpdateStacksQueue())
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

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
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
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
    public void handleRequest_SelfManaged_DeleteStacksNotStarted_InProgress() {

        doReturn(DELETE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksQueue(DELETE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();
        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();
        stackInstancesSet.remove(stackInstances);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksStarted(true)
                .operationId(OPERATION_ID_1)
                .stackInstancesInOperation(DELETE_STACK_INSTANCES_SELF_MANAGED)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);

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
    public void handleRequest_DeleteStacksNotYetStabilized_InProgress() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .deleteStacksStarted(true)
                .templateAnalyzed(true)
                .stackInstancesInOperation(DELETE_STACK_INSTANCES_SELF_MANAGED)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .operationId(OPERATION_ID_2)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .updateStackSetStarted(true)
                .deleteStacksStarted(true)
                .templateAnalyzed(true)
                .stackInstancesInOperation(DELETE_STACK_INSTANCES_SELF_MANAGED)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
                .elapsedTime(BASE_CALLBACK_DELAY_SECONDS)
                .operationId(OPERATION_ID_2)
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
    public void handleRequest_AddStacksNotStarted_InProgress() {

        doReturn(CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksStarted(true)
                .operationId(OPERATION_ID_2)
                .stackInstancesInOperation(DELETE_STACK_INSTANCES_SELF_MANAGED)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();
        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();
        stackInstancesSet.remove(stackInstances);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksStarted(true)
                .addStacksStarted(true)
                .operationId(OPERATION_ID_1)
                .stackInstancesInOperation(CREATE_STACK_INSTANCES_SELF_MANAGED)
                .createStacksQueue(CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES, true);

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
    public void handleRequest_UpdateStacksNotStarted_InProgress() {

        doReturn(UPDATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksStarted(true)
                .addStacksStarted(true)
                .operationId(OPERATION_ID_2)
                .stackInstancesInOperation(CREATE_STACK_INSTANCES_SELF_MANAGED)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        inputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        inputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES, true);
        inputContext.getOperationsStabilizationMap().put(ADD_INSTANCES, true);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inputContext, logger);

        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();
        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();
        stackInstancesSet.remove(stackInstances);

        final CallbackContext outputContext = CallbackContext.builder()
                .updateStackSetStarted(true)
                .templateAnalyzed(true)
                .deleteStacksStarted(true)
                .addStacksStarted(true)
                .updateStacksStarted(true)
                .operationId(OPERATION_ID_1)
                .stackInstancesInOperation(stackInstances)
                .updateStacksQueue(UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
                .build();

        outputContext.getOperationsStabilizationMap().put(STACK_SET_CONFIGS, true);
        outputContext.getOperationsStabilizationMap().put(DELETE_INSTANCES, true);
        outputContext.getOperationsStabilizationMap().put(ADD_INSTANCES, true);

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

        doThrow(OperationInProgressException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final CallbackContext outputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .createStacksQueue(response.getCallbackContext().getCreateStacksQueue())
                .deleteStacksQueue(response.getCallbackContext().getDeleteStacksQueue())
                .updateStacksQueue(response.getCallbackContext().getUpdateStacksQueue())
                .build();

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
