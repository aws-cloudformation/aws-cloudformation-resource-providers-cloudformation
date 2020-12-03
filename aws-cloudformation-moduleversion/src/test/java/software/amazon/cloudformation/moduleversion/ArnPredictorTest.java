package software.amazon.cloudformation.moduleversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.test.AbstractMockTestBase;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArnPredictorTest extends AbstractMockTestBase<CloudFormationClient> {

    private CloudFormationClient client;
    private ListHandler listHandler;
    private ArnPredictor predictor;

    private final String awsAccountId = "123456789012";
    private final String awsPartition = "aws";
    private final String region = "us-west-2";

    private final String moduleName = "My::Test::Resource::MODULE";

    private final String arn1 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000001";
    private final String arn2 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000002";
    private final String arn3 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000003";
    private final String arn4 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000005";
    private final String arn5 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000006";
    private final String arn6 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000007";
    private final String arn7 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000008";
    private final String arn8 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000009";
    private final String arn9 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000011";
    private final String arn10 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000013";
    private final String arn11 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000024";
    private final String arn12 = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000025";
    private final ResourceModel modelResult1 = ResourceModel.builder().arn(arn1).build();
    private final ResourceModel modelResult2 = ResourceModel.builder().arn(arn2).build();
    private final ResourceModel modelResult3 = ResourceModel.builder().arn(arn3).build();
    private final ResourceModel modelResult4 = ResourceModel.builder().arn(arn4).build();
    private final ResourceModel modelResult5 = ResourceModel.builder().arn(arn5).build();
    private final ResourceModel modelResult6 = ResourceModel.builder().arn(arn6).build();
    private final ResourceModel modelResult7 = ResourceModel.builder().arn(arn7).build();
    private final ResourceModel modelResult8 = ResourceModel.builder().arn(arn8).build();
    private final ResourceModel modelResult9 = ResourceModel.builder().arn(arn9).build();
    private final ResourceModel modelResult10 = ResourceModel.builder().arn(arn10).build();
    private final ResourceModel modelResult11 = ResourceModel.builder().arn(arn11).build();
    private final ResourceModel modelResult12 = ResourceModel.builder().arn(arn12).build();

    protected ArnPredictorTest() {
        super(CloudFormationClient.class);
        this.client = getServiceClient();
        when(client.serviceName()).thenReturn("cloudformation");
        this.listHandler = mock(ListHandler.class);
        this.predictor = new ArnPredictor(this.listHandler);
    }

    @Test
    public void handleRequest_BasicSuccess() {
        final String expectedArn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000008";

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null)
                .thenThrow(TypeNotFoundException.builder().build());

        final List<ResourceModel> resourceModelsOut1 = Arrays.asList(modelResult1, modelResult3, modelResult5);
        final List<ResourceModel> resourceModelsOut2 = Arrays.asList(modelResult2, modelResult4, modelResult6);
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut1)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut2)
                        .status(OperationStatus.SUCCESS)
                        .build());

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        final String predictedArn = predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy);

        verify(client, times(2))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(2))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        assertThat(predictedArn).isEqualTo(expectedArn);
    }

    @Test
    public void handleRequest_BasicSuccess_WithNextTokens() {
        final String expectedArn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000026";

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null)
                .thenThrow(TypeNotFoundException.builder().build());

        final List<ResourceModel> resourceModelsOut1 = Arrays.asList(modelResult1, modelResult3, modelResult5);
        final List<ResourceModel> resourceModelsOut2 = Arrays.asList(modelResult2, modelResult4, modelResult6);
        final List<ResourceModel> resourceModelsOut3 = Arrays.asList(modelResult7, modelResult9, modelResult11);
        final List<ResourceModel> resourceModelsOut4 = Arrays.asList(modelResult8, modelResult10, modelResult12);
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .nextToken("dummy next token")
                        .resourceModels(resourceModelsOut1)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut3)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .nextToken("dummy next token")
                        .resourceModels(resourceModelsOut2)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut4)
                        .status(OperationStatus.SUCCESS)
                        .build());

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        final String predictedArn = predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy);

        verify(client, times(2))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(4))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        assertThat(predictedArn).isEqualTo(expectedArn);
    }

    @Test
    public void handleRequest_List_UnexpectedErrorThrown() {
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null);

        final CfnGeneralServiceException exception = new CfnGeneralServiceException("test");
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenThrow(exception);

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        assertThatThrownBy(() -> predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy))
                .hasNoCause()
                .hasMessage("Error occurred during operation 'test'.")
                .isExactlyInstanceOf(CfnGeneralServiceException.class);
        verify(client, times(1))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(1))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_BeforePrediction_Read_NotFound() {
        final String expectedArn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000001";

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenThrow(TypeNotFoundException.builder().build());

        final String predictedArn = predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy);

        verify(client, times(1))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(0))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        assertThat(predictedArn).isEqualTo(expectedArn);
    }

    @Test
    public void handleRequest_BeforePrediction_Read_UnexpectedErrorThrown() {
        final NullPointerException exception = new NullPointerException();
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenThrow(exception);

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        assertThatThrownBy(() -> predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy))
                .hasNoCause()
                .isExactlyInstanceOf(NullPointerException.class);
        verify(client, times(1))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(0))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_AfterPrediction_Read_ModuleVersionExists() {
        final String expectedArn = "arn:aws:cloudformation:us-west-2:123456789012:type/module/My-Test-Resource-MODULE/00000009";

        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null)
                .thenReturn(null)
                .thenThrow(TypeNotFoundException.builder().build());

        final List<ResourceModel> resourceModelsOut1 = Arrays.asList(modelResult1, modelResult3, modelResult5);
        final List<ResourceModel> resourceModelsOut2 = Arrays.asList(modelResult2, modelResult4, modelResult6);
        final List<ResourceModel> resourceModelsOut3 = Arrays.asList(modelResult2, modelResult4, modelResult6, modelResult7);
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut2)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut1)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut3)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut1)
                        .status(OperationStatus.SUCCESS)
                        .build());

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        final String predictedArn = predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy);

        verify(client, times(3))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(4))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        assertThat(predictedArn).isEqualTo(expectedArn);
    }

    @Test
    public void handleRequest_AfterPrediction_Read_UnexpectedErrorThrown() {
        final NullPointerException exception = new NullPointerException();
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null)
                .thenThrow(exception);

        final List<ResourceModel> resourceModelsOut1 = Arrays.asList(modelResult1, modelResult3, modelResult5);
        final List<ResourceModel> resourceModelsOut2 = Arrays.asList(modelResult2, modelResult4, modelResult6);
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut2)
                        .status(OperationStatus.SUCCESS)
                        .build())
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut1)
                        .status(OperationStatus.SUCCESS)
                        .build());

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        assertThatThrownBy(() -> predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy))
                .hasNoCause()
                .isExactlyInstanceOf(NullPointerException.class);
        verify(client, times(2))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(2))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
    }

    @Test
    public void handleRequest_Failure_ExceededRetries() {
        when(client.describeType(any(DescribeTypeRequest.class)))
                .thenReturn(null);

        final List<ResourceModel> resourceModelsOut = Arrays.asList(modelResult1, modelResult3, modelResult5);
        when(listHandler.handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class)))
                .thenReturn(ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(resourceModelsOut)
                        .status(OperationStatus.SUCCESS)
                        .build());

        final ResourceModel model = ResourceModel.builder()
                .moduleName(moduleName)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(awsAccountId)
                .awsPartition(awsPartition)
                .desiredResourceState(model)
                .region(region)
                .build();

        final String predictedArn = predictor.predictArn(proxy, request, new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), loggerProxy);

        verify(client, times(4))
                .describeType(any(DescribeTypeRequest.class));
        verify(listHandler, times(6))
                .handleRequest(any(AmazonWebServicesClientProxy.class), any(), any(CallbackContext.class), any(), any(Logger.class));
        assertThat(predictedArn).isNull();
    }
}
