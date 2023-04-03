package software.amazon.cloudformation.moduleversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;

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
    * Request to create a resource
    * @param model resource model
    * @return the aws service request to create a resource
    */
    static RegisterTypeRequest translateToCreateRequest(@NonNull final ResourceModel model, @NonNull final String clientRequestToken) {
        return RegisterTypeRequest.builder()
                .schemaHandlerPackage(model.getModulePackage())
                .type("MODULE")
                .typeName(model.getModuleName())
                .clientRequestToken(clientRequestToken)
                .build();
    }

    /**
     * Request to describe the resource registration
     * @param registrationToken identifier token created on resource registration
     * @return the aws service request to describe the resource registration
     * */
    static DescribeTypeRegistrationRequest translateToDescribeTypeRegistrationRequest(@NonNull final String registrationToken) {
        return DescribeTypeRegistrationRequest.builder()
                .registrationToken(registrationToken)
                .build();
    }

    /**
    * Request to read a resource
    * @param model resource model
    * @return the aws service request to describe a resource
    */
    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        return DescribeTypeRequest.builder()
                .arn(model.getArn())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param response the aws service describe resource response
     * @return resource model
     */
    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse response) {
        String documentationUrl = response.documentationUrl() != null ? response.documentationUrl() : "";
        return ResourceModel.builder()
                .arn(response.arn())
                .description(response.description())
                .documentationUrl(documentationUrl)
                .isDefaultVersion(response.isDefaultVersion())
                .moduleName(response.typeName())
                .schema(response.schema())
                .timeCreated(response.timeCreated() != null ? response.timeCreated().toString() : null)
                .versionId(response.arn().substring(response.arn().lastIndexOf('/') + 1))
                .visibility(response.visibilityAsString())
                .build();
    }

    /**
    * Request to delete a resource
    * @param model resource model
    * @return the aws service request to delete a resource
    */
    static DeregisterTypeRequest translateToDeleteRequest(@NonNull final ResourceModel model) {
        DeregisterTypeRequest.Builder builder = DeregisterTypeRequest.builder();
        if (!model.getIsDefaultVersion()) {
            builder.arn(model.getArn()); // deregister the specific version of the module
        } else {
            builder.type("MODULE").typeName(model.getModuleName()); // attempt to deregister the module itself
        }
        return builder.build();
    }

    /**
    * Request to list resources
    * @param nextToken token passed to the aws service list resources request
    * @return awsRequest the aws service request to list resources within aws account
    */
    static ListTypesRequest translateToListTypesRequest(final String nextToken) {
        return ListTypesRequest.builder()
                .maxResults(LIST_MAX_RESULTS)
                .nextToken(nextToken)
                .build();
    }

    /**
    * Translates resource objects from sdk into a resource model (primary identifier only)
    * @param response the aws service describe resource response
    * @return list of resource models
    */
    static List<ResourceModel> translateFromListTypesResponse(@NonNull final ListTypesResponse response) {
        return streamOfOrEmpty(response.typeSummaries())
                .filter(summary -> summary.typeAsString().equals("MODULE"))
                .map(summary -> ResourceModel.builder()
                        .moduleName(summary.typeName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Request to list resources
     * @param model model for which the versions will be retrieved
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListTypeVersionsRequest translateToListTypeVersionsRequest(
            @NonNull final ResourceModel model,
            final String nextToken,
            final DeprecatedStatus deprecatedStatus) {
        return ListTypeVersionsRequest.builder()
                .deprecatedStatus(deprecatedStatus)
                .type("MODULE")
                .typeName(model.getModuleName())
                .maxResults(LIST_MAX_RESULTS)
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     * @param response the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListTypeVersionsResponse(@NonNull final ListTypeVersionsResponse response) {
        return streamOfOrEmpty(response.typeVersionSummaries())
                .map(summary -> ResourceModel.builder()
                        .arn(summary.arn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
