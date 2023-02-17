package software.amazon.cloudformation.stack;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.time.Duration;

public class ClientBuilder {

  private static final BackoffStrategy STACK_BACKOFF_THROTTLING_STRATEGY =
      EqualJitterBackoffStrategy.builder()
          .baseDelay(Duration.ofMillis(2000)) //1st retry is ~2 sec
          .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF) //default is 20s
          .build();
  private static final RetryPolicy STACK_RETRY_POLICY =
      RetryPolicy.builder()
          .numRetries(4)
          .retryCondition(RetryCondition.defaultRetryCondition())
          .throttlingBackoffStrategy(STACK_BACKOFF_THROTTLING_STRATEGY)
          .build();
  public static CloudFormationClient getClient() {
    return CloudFormationClient.builder()
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .overrideConfiguration(ClientOverrideConfiguration.builder()
            .retryPolicy(STACK_RETRY_POLICY)
            .build())
        .build();
  }
}
