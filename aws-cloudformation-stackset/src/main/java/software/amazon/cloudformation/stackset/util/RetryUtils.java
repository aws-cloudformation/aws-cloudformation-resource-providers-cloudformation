package software.amazon.cloudformation.stackset.util;

import com.amazonaws.util.StringUtils;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.proxy.Logger;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class RetryUtils {
    private final static int MAX_RETRIES = 3;
    private final Logger logger;
    private int retryCount = 0;

    RetryUtils(Logger logger, int retryCount) {
        this.logger = logger;
        this.retryCount = retryCount;
    }

    public <T> T runWithRetry(Supplier<T> function) {
        while (true) {
            try {
                return function.get();
            } catch (CloudFormationException e) {
                if (retryCount >= MAX_RETRIES) {
                    logger.log("Reached max retry limit");
                    throw e;
                }
                final String errorMessage = e.getMessage();
                if (StringUtils.isNullOrEmpty(errorMessage) || !errorMessage.contains("Rate exceeded")) {
                    logger.log("Failed interacting with CloudFormation with exception " + e.toString());
                    throw e;
                }
            }
            try {
                long waitTime = getWaitTimeExp(retryCount++);
                logger.log("Retrying " + retryCount + " attempts, waiting for " + waitTime + " ms");
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.log("Interrupted while retrying");
                throw new CfnInternalFailureException(e);
            }
        }
    }

    private long getWaitTimeExp(int retryCount) {
        long waitTime = ((long) Math.pow(2, retryCount) * 500L);
        return waitTime;
    }
}
