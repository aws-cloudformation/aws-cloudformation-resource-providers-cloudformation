package software.amazon.cloudformation.stackset.util;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    private ClientBuilder() {}

    /**
     * Get CloudFormationClient for requests to interact with StackSet client
     * @return {@link CloudFormationClient}
     */
    public static CloudFormationClient getClient() {
        return CloudFormationClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

    /**
     * Gets S3 client for requests to interact with getting/validating template content
     * if {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateURL()} is passed in
     * @param awsCredentialsProvider {@link AwsCredentialsProvider}
     * @return {@link S3Client}
     */
    public static S3Client getS3Client(final AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client.builder()
                .credentialsProvider(awsCredentialsProvider)
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}