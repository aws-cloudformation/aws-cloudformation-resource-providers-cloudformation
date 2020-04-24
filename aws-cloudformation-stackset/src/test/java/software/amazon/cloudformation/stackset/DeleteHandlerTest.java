package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static software.amazon.cloudformation.stackset.util.Stabilizer.BASE_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.TestUtils.DELETE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_RUNNING_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    private DeleteHandler handler;

    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new DeleteHandler();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SELF_MANAGED_MODEL)
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_MODEL)
                .build();

        doReturn(OPERATION_SUCCEED_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stackSetCreated(true)
                .deleteStackInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
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
    public void handleRequest_DeleteNotYetStarted_InProgress() {

        doReturn(DELETE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final Set<StackInstances> stackInstancesSet = request.getDesiredResourceState().getStackInstancesGroup();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final StackInstances stackInstances = response.getCallbackContext().getStackInstancesInOperation();

        stackInstancesSet.remove(stackInstances);

        final CallbackContext outputContext = CallbackContext.builder()
                .deleteStackInstancesStarted(true)
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .deleteStackInstancesQueue(new LinkedList<>(stackInstancesSet))
                .stackInstancesInOperation(stackInstances)
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
    public void handleRequest_DeleteNotYetStabilized_InProgress() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .deleteStackInstancesStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        final CallbackContext outputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .deleteStackInstancesStarted(true)
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
    public void handlerRequest_DeleteStackSet_StackSetNotFoundException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_MODEL)
                .build();

        final CallbackContext inputContext = CallbackContext.builder()
                .templateAnalyzed(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        doThrow(StackSetNotFoundException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, inputContext, logger));

    }

    @Test
    public void handlerRequest_DeleteInstances_StackSetNotFoundException() {

        doThrow(StackSetNotFoundException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }
}
