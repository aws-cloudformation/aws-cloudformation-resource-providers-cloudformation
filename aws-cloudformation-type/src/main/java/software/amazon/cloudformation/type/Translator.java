package software.amazon.cloudformation.type;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    static RegisterTypeRequest translateToCreateRequest(@NonNull final ResourceModel model) {
        final RegisterTypeRequest.Builder builder = RegisterTypeRequest.builder()
            .executionRoleArn(model.getExecutionRoleArn())
            .schemaHandlerPackage(model.getSchemaHandlerPackage())
            .type(model.getType())
            .typeName(model.getTypeName());

        if (model.getLoggingConfig() != null) {
            builder.loggingConfig(translateToSDK(model.getLoggingConfig()));
        }

         return builder.build();
    }

    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        return DescribeTypeRequest.builder()
            .type(model.getType())
            .typeName(model.getTypeName())
            .build();
    }

    static DeregisterTypeRequest translateToDeleteRequest(@NonNull final ResourceModel model) {
        return DeregisterTypeRequest.builder()
            .type(model.getType())
            .typeName(model.getTypeName())
            .build();
    }

    static ListTypesRequest translateToListRequest(final String nextToken) {
        return ListTypesRequest.builder()
            .maxResults(50)
            .nextToken(nextToken)
            .build();
    }

    static ResourceModel translateForRead(@NonNull final DescribeTypeResponse response) {
        final ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
            .arn(response.arn())
            .defaultVersionId(response.defaultVersionId())
            .deprecatedStatus(response.deprecatedStatusAsString())
            .description(response.description())
            .documentationUrl(response.documentationUrl())
            .executionRoleArn(response.executionRoleArn())
            .provisioningType(response.provisioningTypeAsString())
            .schema(response.schema())
            .sourceUrl(response.sourceUrl())
            .type(response.typeAsString())
            .typeName(response.typeName())
            .visibility(response.visibilityAsString());

        if (response.lastUpdated() != null) {
            builder.lastUpdated(response.lastUpdated().toString());
        }
        if (response.timeCreated() != null) {
            builder.timeCreated(response.timeCreated().toString());
        }
        if (response.loggingConfig() != null) {
            builder.loggingConfig(translateFromSDK(response.loggingConfig()));
        }

        return builder.build();
    }

    static List<ResourceModel> translateForList(@NonNull final ListTypesResponse response) {
        return streamOfOrEmpty(response.typeSummaries())
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

    static software.amazon.awssdk.services.cloudformation.model.LoggingConfig translateToSDK(@NonNull final LoggingConfig loggingConfig) {
        return software.amazon.awssdk.services.cloudformation.model.LoggingConfig.builder()
            .logGroupName(loggingConfig.getLogGroupName())
            .logRoleArn(loggingConfig.getLogRoleArn())
            .build();
    }

    static LoggingConfig translateFromSDK(@NonNull final software.amazon.awssdk.services.cloudformation.model.LoggingConfig loggingConfig) {
        return LoggingConfig.builder()
            .logGroupName(loggingConfig.logGroupName())
            .logRoleArn(loggingConfig.logRoleArn())
            .build();

    }
}
