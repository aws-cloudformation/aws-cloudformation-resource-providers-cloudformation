package software.amazon.cloudformation.moduleversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

/**
 * This class is a temporary solution to the problem created by the combination of:
 * (1) the primary identifier of a ModuleVersion must be the ARN
 * (2) the ModuleVersion ARN is not created until registration has completed stabilization
 * (3) the primary identifier must be returned in "in-progress" Create Handler responses as per contract tests and for stack cleanup
 */
class ArnPredictor {

    private static final int MAX_RETRIES = 2;

    private ListHandler listHandler;

    ArnPredictor() {
        this(new ListHandler());
    }

    ArnPredictor(final ListHandler listHandler) {
        this.listHandler = listHandler;
    }

    /**
     * This method lists all LIVE and DEPRECATED versions of a module, outputting an ARN with a version number equal to
     * the maximum version number found incremented by one.
     */
    String predictArn(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final String moduleName = request.getDesiredResourceState().getModuleName();

        // brief sleep to give module versions time to propagate in case versions are being registered in quick succession
        try {
            Thread.sleep(2000);
        } catch (final InterruptedException exception) {
            logger.log(String.format("Thread's sleep after ARN prediction for module %s interrupted:\n%s",
                    moduleName, Arrays.toString(exception.getStackTrace())));
        }

        final String moduleArn = generateModuleArn(request);

        if (!moduleExists(moduleArn, proxyClient, logger)) {
            logger.log(String.format("Module %s does not exist yet; new module version is the first version of module", moduleName));
            return generateModuleVersionArn(moduleArn, 1);
        }

        callbackContext.setDeprecatedStatus(DeprecatedStatus.LIVE);
        int latestVersion = 0;
        String moduleVersionArn = null;
        int retryCount = 0;
        ProgressEvent<ResourceModel, CallbackContext> progress;
        List<ResourceModel> models;
        while (moduleVersionArn == null && retryCount <= MAX_RETRIES) {
            progress = listHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

            models = progress.getResourceModels();
            if (models != null) {
                logger.log(String.format("%d %s module versions were found for module %s after listing type versions",
                        models.size(), callbackContext.getDeprecatedStatus().toString().toLowerCase(), moduleName));

                int max = models.stream()
                        .map(model -> model.getArn().substring(model.getArn().lastIndexOf("/") + 1))
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(latestVersion);
                latestVersion = Math.max(latestVersion, max);
            }

            request.setNextToken(progress.getNextToken());

            if (request.getNextToken() != null) continue;
            if (callbackContext.getDeprecatedStatus() == DeprecatedStatus.LIVE) {
                // check any deprecated versions as well
                callbackContext.setDeprecatedStatus(DeprecatedStatus.DEPRECATED);
                continue;
            }
            moduleVersionArn = generateModuleVersionArn(moduleArn, latestVersion + 1);
            logger.log(String.format("Predicted ARN for new module version of module %s to be %s", moduleName, moduleVersionArn));

            // additional check to see if module version with predicted ARN already exists
            if (moduleVersionExists(moduleVersionArn, proxyClient, logger)) {
                logger.log(String.format("A module version already exists for module %s with ARN %s; %d ARN prediction retries remaining\n" +
                        "This event may have been caused by module versions being registered in quick succession or concurrently", moduleName, moduleVersionArn, MAX_RETRIES - retryCount));
                callbackContext.setDeprecatedStatus(DeprecatedStatus.LIVE);
                latestVersion = 0;
                moduleVersionArn = null;
                retryCount++;
            }
        }
        callbackContext.setDeprecatedStatus(null);
        return moduleVersionArn;
    }

    private boolean moduleVersionExists(
            final String arn,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {
        return moduleExists(arn, proxyClient, logger);
    }

    private boolean moduleExists(
            final String arn,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(
                    DescribeTypeRequest.builder().arn(arn).build(),
                    proxyClient.client()::describeType);
        } catch (final TypeNotFoundException exception) {
            return false;
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Registry exception during read to check if module with ARN %s exists for ARN prediction:\n%s",
                    arn, Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return true;
    }

    private String generateModuleArn(final ResourceHandlerRequest<ResourceModel> request) {
        return String.format("arn:%s:cloudformation:%s:%s:type/module/%s",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                request.getDesiredResourceState().getModuleName().replace("::", "-"));
    }

    private String generateModuleVersionArn(final String moduleArn, final int version) {
        return String.format("%s/%08d", moduleArn, version);
    }
}
