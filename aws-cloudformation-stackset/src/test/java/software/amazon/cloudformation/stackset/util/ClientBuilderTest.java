package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientBuilderTest {

    @Test
    public void testCloudFormationRetryConditionRetryOnCloudFormationThrottlingException() {
        final AwsServiceException exception = CloudFormationException.builder()
                .message("Rate exceeded")
                .statusCode(400)
                .build();
        final RetryPolicyContext retryPolicyContext = RetryPolicyContext.builder().exception(exception).build();
        assertThat(new ClientBuilder.CloudFormationRetryCondition().shouldRetry(retryPolicyContext)).isTrue();
    }
}
