package software.amazon.cloudformation.resourceversionalias;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class ClientBuilder {
    public static CloudFormationClient getClient() {
        return CloudFormationClient.builder()
            .build();
    }
}