package software.amazon.cloudformation.moduledefaultversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

    /**
    * Request to read a resource
    * @param model resource model
    * @return awsRequest the aws service request to describe a resource
    */
    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        final DescribeTypeRequest.Builder builder = DescribeTypeRequest.builder();
        if (model.getArn() == null) {
            builder.type("MODULE")
                    .typeName(model.getModuleName())
                    .versionId(model.getVersionId());
        } else {
            builder.arn(model.getArn());
        }
        return builder.build();
    }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse response) {
    return ResourceModel.builder()
            .arn(response.arn())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static SetTypeDefaultVersionRequest translateToCreateRequest(@NonNull final ResourceModel model) {
      final SetTypeDefaultVersionRequest.Builder builder = SetTypeDefaultVersionRequest.builder();
      if (model.getArn() == null) {
          builder.type("MODULE")
                  .typeName(model.getModuleName())
                  .versionId(model.getVersionId());
      } else {
          builder.arn(model.getArn());
      }
      return builder.build();
  }
}
