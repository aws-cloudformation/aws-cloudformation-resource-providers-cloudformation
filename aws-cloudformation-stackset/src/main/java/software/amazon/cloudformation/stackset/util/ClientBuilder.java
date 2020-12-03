package software.amazon.cloudformation.stackset.util;

import com.amazonaws.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    private ClientBuilder() {
    }

    public static CloudFormationClient getClient() {
        return LazyHolder.SERVICE_CLIENT;
    }

    /**
     * Get CloudFormationClient for requests to interact with StackSet client
     *
     * @return {@link CloudFormationClient}
     */
    private static class LazyHolder {

        private static final Integer MAX_RETRIES = 5;

        public static CloudFormationClient SERVICE_CLIENT = CloudFormationClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(new RetryCondition[]{
                                        RetryCondition.defaultRetryCondition(),
                                        CloudFormationRetryCondition.create()
                                }))
                                .build())
                        .build())
                .build();
    }

    /**
     * CloudFormation Throttling Exception StatusCode is 400 while default throttling code is 429
     * https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/exception/SdkServiceException.java#L91
     * which means we would need to customize a RetryCondition
     */
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class CloudFormationRetryCondition implements RetryCondition {

        public static CloudFormationRetryCondition create() {
            return new CloudFormationRetryCondition();
        }

        @Override
        public boolean shouldRetry(RetryPolicyContext context) {
            final String errorMessage = context.exception().getMessage();
            if (StringUtils.isNullOrEmpty(errorMessage)) return false;
            return errorMessage.contains("Rate exceeded");
        }
    }

}
