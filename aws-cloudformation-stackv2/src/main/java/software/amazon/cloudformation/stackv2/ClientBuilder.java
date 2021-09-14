package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.core.SdkClient;
// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceClient
// import software.amazon.awssdk.services.yourservice.YourServiceClient;
// import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  /*
  TODO: uncomment the following, replacing YourServiceClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9

  public static YourServiceClient getClient() {
    return YourServiceClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }
  */

  // TODO: remove this implementation once you have uncommented the above
  public static SdkClient getClient() {
    return null;
  }
}
