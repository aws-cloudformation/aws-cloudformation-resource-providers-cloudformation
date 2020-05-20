package software.amazon.cloudformation.stackset.util;

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
        public static CloudFormationClient SERVICE_CLIENT = CloudFormationClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
