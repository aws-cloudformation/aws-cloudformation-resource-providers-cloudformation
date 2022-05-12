package software.amazon.cloudformation.hookdefaultversion;

import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static CloudFormationClient getClient() {
        return CloudFormationClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(c -> c.retryPolicy(RetryMode.STANDARD))
            .build();
    }
}
