package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.LoggerProxy;

import static software.amazon.cloudformation.stackset.util.TestUtils.RATE_EXCEEDED_EXCEPTION;

public class RetryUtilsTest {

    Logger logger;

    @BeforeEach
    public void setup() {
        logger = new LoggerProxy();
    }

    @Test
    public void retryHappyFlowTest() {
        new RetryUtils(logger).runWithRetry(() -> true);
    }

    @Test
    public void retryGotExceptionTest() {
        Assertions.assertThrows(CloudFormationException.class, () -> new RetryUtils(logger).runWithRetry(() -> {
            throw CloudFormationException.builder().message("Unknown").build();
        }));
        Assertions.assertThrows(CloudFormationException.class, () -> new RetryUtils(logger).runWithRetry(() -> {
            throw CloudFormationException.builder().build();
        }));
        Assertions.assertThrows(RuntimeException.class, () -> new RetryUtils(logger).runWithRetry(() -> {
            throw new RuntimeException();
        }));
    }

    @Test
    public void retryWithVerifyRetryOnRateExceededTest() {
        Assertions.assertThrows(CloudFormationException.class,
                () -> new RetryUtils(logger).runWithRetry(() -> {
                    throw RATE_EXCEEDED_EXCEPTION;
                }));
    }

    @Test
    public void retryWithVerifyMaxAttemptsTest() {
        Assertions.assertThrows(CloudFormationException.class,
                () -> new RetryUtils(logger, 3).runWithRetry(() -> {
                    throw RATE_EXCEEDED_EXCEPTION;
                }));
    }

    @Test
    public void retryWithVerifyInterruptedExceptionTest() {
        Thread.currentThread().interrupt();
        Assertions.assertThrows(CfnInternalFailureException.class,
                () -> new RetryUtils(logger, 1).runWithRetry(() -> {
                    throw RATE_EXCEEDED_EXCEPTION;
                }));
    }
}
