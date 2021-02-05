package software.amazon.cloudformation.resourceversion;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
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
        logger.log("Reading Arn: " + model.getArn());

        return DescribeTypeRequest.builder()
                .arn(model.getArn())
                .build();
    }

    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse awsResponse) {
        final ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .arn(awsResponse.arn())
                .typeArn(awsResponse.arn().substring(0, awsResponse.arn().lastIndexOf("/")))
                .executionRoleArn(awsResponse.executionRoleArn())
                .isDefaultVersion(awsResponse.isDefaultVersion())
                .provisioningType(awsResponse.provisioningTypeAsString())
                .typeName(awsResponse.typeName())
                .versionId(awsResponse.arn().substring(awsResponse.arn().lastIndexOf('/') + 1))
                .visibility(awsResponse.visibilityAsString());

        if (awsResponse.loggingConfig() != null) {
            builder.loggingConfig(translateFromSDK(awsResponse.loggingConfig()));
        }

        return builder.build();
    }

    static DeregisterTypeRequest translateToDeleteRequest(@NonNull final ResourceModel model,
                                                          @NonNull final Logger logger) {
        if (model.getIsDefaultVersion()) {
            logger.log(String.format("De-registering the default version :: TypeName is %s ", model.getTypeName()));
            return DeregisterTypeRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(model.getTypeName())
                    .build();
        } else {
            logger.log(String.format("De-registering the default version :: Arn is %s ", model.getArn()));
            return DeregisterTypeRequest.builder()
                    .arn(model.getArn())
                    .build();
        }
    }

    static ListTypeVersionsRequest translateToListRequest(ResourceModel resourceModel, final String nextToken) {

        if (StringUtils.isNullOrEmpty(resourceModel.getTypeArn())) {
            return ListTypeVersionsRequest.builder()
                    .maxResults(50)
                    .nextToken(nextToken)
                    .type(RegistryType.RESOURCE)
                    .typeName(resourceModel.getTypeName())
                    .build();
        } else {
            return ListTypeVersionsRequest.builder()
                    .maxResults(50)
                    .nextToken(nextToken)
                    .arn(resourceModel.getTypeArn())
                    .build();
        }
    }

    static List<ResourceModel> translateFromListResponse(@NonNull final ListTypeVersionsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.typeVersionSummaries())
                .map(typeSummary -> ResourceModel.builder()
                        .arn(typeSummary.arn())
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
