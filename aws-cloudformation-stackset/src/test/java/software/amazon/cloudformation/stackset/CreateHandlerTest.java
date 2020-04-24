package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.InsufficientCapabilitiesException;
import software.amazon.awssdk.services.cloudformation.model.LimitExceededException;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.LinkedList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.amazon.cloudformation.stackset.util.EnumUtils.Operations.ADD_INSTANCES;
import static software.amazon.cloudformation.stackset.util.Stabilizer.BASE_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.EXECUTION_TIMEOUT_SECONDS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.MAX_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.INVALID_SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_RUNNING_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_STOPPED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED_MODEL;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    private CreateHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new CreateHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .build();

        inputContext.getOperationsStabilizationMap().put(ADD_INSTANCES, true);

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
    public void handleRequest_SelfManaged_CreateNotYetStarted_InProgress() {

        doReturn(CREATE_STACK_SET_RESPONSE,
                CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();
        stackInstancesSet.remove(stackInstances);

        final CallbackContext outputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .operationId(OPERATION_ID_1)
                .createStacksInstancesQueue(new LinkedList<>(stackInstancesSet))
                .stackInstancesInOperation(stackInstances)
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
    public void handleRequest_ServiceManaged_CreateNotYetStarted_InProgress() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SERVICE_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE,
                CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();

        final CallbackContext outputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .operationId(OPERATION_ID_1)
                .stackInstancesInOperation(stackInstances)
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
    public void handleRequest_CreateNotYetStabilized_InProgress() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .operationId(OPERATION_ID_1)
                .templateAnalyzed(true)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        final CallbackContext outputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .elapsedTime(BASE_CALLBACK_DELAY_SECONDS)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS + 1)
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
    public void handleRequest_CreateWithDuplicatedInstances_InvalidRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(INVALID_SELF_MANAGED_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_OperationStopped_CfnNotStabilizedException() {

        doReturn(OPERATION_STOPPED_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, inputContext, logger));
    }

    @Test
    public void handleRequest_OperationTimesOut_CfnNotStabilizedException() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .elapsedTime(EXECUTION_TIMEOUT_SECONDS)
                .currentDelaySeconds(MAX_CALLBACK_DELAY_SECONDS)
                .build();

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, inputContext, logger));
    }

    @Test
    public void handleRequest_OperationMaxRetries_CfnNotStabilizedException() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .addStacksInstancesStarted(true)
                .operationId(OPERATION_ID_1)
                .elapsedTime(EXECUTION_TIMEOUT_SECONDS)
                .currentDelaySeconds(MAX_CALLBACK_DELAY_SECONDS)
                .build();

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, inputContext, logger));
    }

    @Test
    public void handlerRequest_AlreadyExistsException() {

        doThrow(AlreadyExistsException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnAlreadyExistsException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_LimitExceededException() {

        doThrow(LimitExceededException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnServiceLimitExceededException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_InsufficientCapabilitiesException() {

        doThrow(InsufficientCapabilitiesException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_StackSetNotFoundException() {

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        doThrow(StackSetNotFoundException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackInstancesRequest.class), any());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_OperationInProgressException() {

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        doThrow(OperationInProgressException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackInstancesRequest.class), any());


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final CallbackContext outputContext = CallbackContext.builder()
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .stackSetCreated(true)
                .templateAnalyzed(true)
                .createStacksInstancesQueue(response.getCallbackContext().getCreateStacksInstancesQueue())
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
