package software.amazon.cloudformation.resourceversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Translator {

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static RegisterTypeRequest translateToCreateRequest(@NonNull final ResourceModel model) {
        final RegisterTypeRequest.Builder builder = RegisterTypeRequest.builder()
                .executionRoleArn(model.getExecutionRoleArn())
                .schemaHandlerPackage(model.getSchemaHandlerPackage())
                .type(RegistryType.RESOURCE)
                .typeName(model.getTypeName());

        if (model.getLoggingConfig() != null) {
            builder.loggingConfig(translateToSDK(model.getLoggingConfig()));
        }

        return builder.build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model,
                                                      @NonNull final Logger logger) {
        logger.log("Reading Arn: " + model.getArn());

        return DescribeTypeRequest.builder()
                .arn(model.getArn())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     *
     * @param awsResponse   the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse awsResponse) {
        final ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .arn(awsResponse.arn())
                .description(awsResponse.description())
                .documentationUrl(awsResponse.documentationUrl())
                .executionRoleArn(awsResponse.executionRoleArn())
                .isDefaultVersion(awsResponse.isDefaultVersion())
                .provisioningType(awsResponse.provisioningTypeAsString())
                .schema(awsResponse.schema())
                .sourceUrl(awsResponse.sourceUrl())
                .typeName(awsResponse.typeName())
                .versionId(awsResponse.arn().substring(awsResponse.arn().lastIndexOf('/') + 1))
                .visibility(awsResponse.visibilityAsString());

        if (awsResponse.lastUpdated() != null) {
            builder.lastUpdated(awsResponse.lastUpdated().toString());
        }
        if (awsResponse.timeCreated() != null) {
            builder.timeCreated(awsResponse.timeCreated().toString());
        }
        if (awsResponse.loggingConfig() != null) {
            builder.loggingConfig(translateFromSDK(awsResponse.loggingConfig()));
        }
        return builder.build();
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeregisterTypeRequest translateToDeleteRequest(@NonNull final ResourceModel model,
                                                          @NonNull final Logger logger) {
        if (model.getIsDefaultVersion()) {
            logger.log("De-registering default version");
            return DeregisterTypeRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(model.getTypeName())
                    .build();
        } else {
            logger.log("De-registering version");
            return DeregisterTypeRequest.builder()
                    .arn(model.getArn())
                    .build();
        }
    }

    /**
     * Request to list all types
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListTypesRequest translateToListRequest(final String nextToken) {
        return ListTypesRequest.builder()
                .maxResults(50)
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(@NonNull final ListTypesResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.typeSummaries())
                .map(typeSummary -> ResourceModel.builder()
                        .arn(typeSummary.typeArn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    private static software.amazon.awssdk.services.cloudformation.model.LoggingConfig translateToSDK(@NonNull final LoggingConfig loggingConfig) {
        return software.amazon.awssdk.services.cloudformation.model.LoggingConfig.builder()
                .logGroupName(loggingConfig.getLogGroupName())
                .logRoleArn(loggingConfig.getLogRoleArn())
                .build();
    }

    private static LoggingConfig translateFromSDK(@NonNull final software.amazon.awssdk.services.cloudformation.model.LoggingConfig loggingConfig) {
        return LoggingConfig.builder()
                .logGroupName(loggingConfig.logGroupName())
                .logRoleArn(loggingConfig.logRoleArn())
                .build();

    }

    public static ListTypeVersionsRequest translateToListTypeVersionsRequest(ResourceModel resourceModel, String marker, DeprecatedStatus deprecatedStatus) {
        return ListTypeVersionsRequest.builder()
                .nextToken(marker)
                .deprecatedStatus(deprecatedStatus)
                .typeName(resourceModel.getTypeName())
                .type(RegistryType.RESOURCE)
                .build();
    }
}
