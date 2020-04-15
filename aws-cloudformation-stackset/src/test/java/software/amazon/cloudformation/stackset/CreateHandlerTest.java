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
import software.amazon.cloudformation.stackset.util.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.amazon.cloudformation.stackset.util.Stabilizer.BASE_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.EXECUTION_TIMEOUT_SECONDS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.MAX_CALLBACK_DELAY_SECONDS;
import static software.amazon.cloudformation.stackset.util.Stabilizer.MAX_RETRIES;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_INSTANCES_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.CREATE_STACK_SET_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.LOGICAL_ID;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_ID_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_RUNNING_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_STOPPED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.OPERATION_SUCCEED_RESPONSE;
import static software.amazon.cloudformation.stackset.util.TestUtils.REQUEST_TOKEN;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.SIMPLE_TEMPLATE_BODY_MODEL;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    private CreateHandler handler;

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
        handler = CreateHandler.builder().validator(validator).build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        doReturn(OPERATION_SUCCEED_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stabilizationStarted(true)
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
    public void handleRequest_TemplateUrl_CreateNotYetStarted_InProgress() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doReturn(CREATE_STACK_SET_RESPONSE,
                CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext outputContext = CallbackContext.builder()
                .stabilizationStarted(true)
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
    public void handleRequest_TemplateBody_CreateNotYetStarted_InProgress() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(SIMPLE_TEMPLATE_BODY_MODEL)
                .logicalResourceIdentifier(LOGICAL_ID)
                .clientRequestToken(REQUEST_TOKEN)
                .build();

        doReturn(CREATE_STACK_SET_RESPONSE,
                CREATE_STACK_INSTANCES_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext outputContext = CallbackContext.builder()
                .stabilizationStarted(true)
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
    public void handleRequest_CreateNotYetStabilized_InProgress() {

        doReturn(OPERATION_RUNNING_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stabilizationStarted(true)
                .operationId(OPERATION_ID_1)
                .currentDelaySeconds(BASE_CALLBACK_DELAY_SECONDS)
                .build();

        final CallbackContext outputContext = CallbackContext.builder()
                .stabilizationStarted(true)
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
    public void handleRequest_OperationStopped_CfnNotStabilizedException() {

        doReturn(OPERATION_STOPPED_RESPONSE).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        final CallbackContext inputContext = CallbackContext.builder()
                .stabilizationStarted(true)
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
                .stabilizationStarted(true)
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
                .stabilizationStarted(true)
                .operationId(OPERATION_ID_1)
                .retries(MAX_RETRIES + 1)
                .currentDelaySeconds(MAX_CALLBACK_DELAY_SECONDS)
                .build();

        assertThrows(CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, inputContext, logger));
    }

    @Test
    public void handlerRequest_AlreadyExistsException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doThrow(AlreadyExistsException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnAlreadyExistsException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_LimitExceededException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doThrow(LimitExceededException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnServiceLimitExceededException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_InsufficientCapabilitiesException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doThrow(InsufficientCapabilitiesException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        assertThrows(CfnInvalidRequestException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_StackSetNotFoundException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        doThrow(StackSetNotFoundException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackInstancesRequest.class), any());

        assertThrows(CfnNotFoundException.class,
                () -> handler.handleRequest(proxy, request, null, logger));

    }

    @Test
    public void handlerRequest_OperationInProgressException() {

        doNothing().when(validator).validateTemplate(any(), any(), any(), any());

        doReturn(CREATE_STACK_SET_RESPONSE).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackSetRequest.class), any());

        doThrow(OperationInProgressException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateStackInstancesRequest.class), any());

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
