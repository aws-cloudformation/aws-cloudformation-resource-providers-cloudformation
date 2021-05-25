package software.amazon.cloudformation.moduledefaultversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

    private static final int LIST_MAX_RESULTS = 100;

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

    static ListTypesRequest translateToListRequest(final String nextToken) {
        return ListTypesRequest.builder()
                .maxResults(LIST_MAX_RESULTS)
                .nextToken(nextToken)
                .deprecatedStatus(DeprecatedStatus.LIVE)
                .build();
    }

    static List<ResourceModel> translateFromListTypesResponse(@NonNull final ListTypesResponse response) {
        return streamOfOrEmpty(response.typeSummaries()).filter(summary -> summary.typeAsString()
                .equals("MODULE"))
                .map(summary -> ResourceModel.builder()
                        .moduleName(summary.typeName())
                        .versionId(summary.defaultVersionId())
                        .arn(summary.typeArn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
