package software.amazon.cloudformation.moduledefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
    public static CloudFormationClient getClient() {
        return CloudFormationClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
