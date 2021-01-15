package software.amazon.cloudformation.resourceversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
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

    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model,
                                                      @NonNull final Logger logger) {
        logger.log("Reading Arn: " + model.getTypeVersionArn());

        return DescribeTypeRequest.builder()
                .arn(model.getTypeVersionArn())
                .build();
    }

    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse awsResponse) {
        final ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .typeVersionArn(awsResponse.arn())
                .typeArn(awsResponse.arn().substring(0, awsResponse.arn().lastIndexOf("/")))
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
                    .arn(model.getTypeVersionArn())
                    .build();
        }
    }

    static ListTypesRequest translateToListRequest(final String nextToken) {
        return ListTypesRequest.builder()
                .maxResults(50)
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListResponse(@NonNull final ListTypesResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.typeSummaries())
                .map(typeSummary -> ResourceModel.builder()
                        .typeVersionArn(typeSummary.typeArn())
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

    public static DescribeTypeRegistrationRequest translateToDescribeTypeRegistration(String registrationToken) {
        return DescribeTypeRegistrationRequest.builder()
                .registrationToken(registrationToken)
                .build();
    }
}
