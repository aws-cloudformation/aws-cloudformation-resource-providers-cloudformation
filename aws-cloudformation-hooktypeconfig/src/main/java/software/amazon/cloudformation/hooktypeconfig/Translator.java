package software.amazon.cloudformation.hooktypeconfig;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsError;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsRequest;
import software.amazon.awssdk.services.cloudformation.model.BatchDescribeTypeConfigurationsResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeConfigurationRequest;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationDetails;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationIdentifier;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONObject;

public class Translator {

    public static final String HOOK = "HOOK";

    static SetTypeConfigurationRequest translateToUpdateRequest(@NonNull final ResourceModel model) {
        if (!StringUtils.isNullOrEmpty(model.getTypeArn())){
            return SetTypeConfigurationRequest.builder()
                    .typeArn(model.getTypeArn())
                    .configurationAlias(model.getConfigurationAlias())
                    .configuration(model.getConfiguration())
                    .build();
        }
        else {
            return SetTypeConfigurationRequest.builder()
                    .type(HOOK)
                    .typeName(model.getTypeName())
                    .configurationAlias(model.getConfigurationAlias())
                    .configuration(model.getConfiguration())
                    .build();
        }
    }

    static BatchDescribeTypeConfigurationsRequest translateToReadRequest(@NonNull final ResourceModel model) {
        final TypeConfigurationIdentifier typeConfigurationIdentifier;
        if (!StringUtils.isNullOrEmpty(model.getTypeArn())){
            typeConfigurationIdentifier = TypeConfigurationIdentifier.builder()
                    .typeConfigurationAlias(model.getConfigurationAlias())
                    .typeArn(model.getTypeArn())
                    .build();
        }
        else if (!StringUtils.isNullOrEmpty(model.getTypeName())){
            typeConfigurationIdentifier = TypeConfigurationIdentifier.builder()
                    .type(HOOK)
                    .typeConfigurationAlias(model.getConfigurationAlias())
                    .typeName(model.getTypeName())
                    .build();
        }
        else {
            typeConfigurationIdentifier = TypeConfigurationIdentifier.builder()
                    .typeConfigurationAlias(model.getConfigurationAlias())
                    .typeConfigurationArn(model.getConfigurationArn())
                    .build();
        }
        return BatchDescribeTypeConfigurationsRequest.builder()
                .typeConfigurationIdentifiers(typeConfigurationIdentifier)
                .build();
    }

    static ResourceModel translateFromReadResponse(@NonNull final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse, @NonNull final Logger logger) {
        if (batchDescribeTypeConfigurationsResponse.errors().size() > 0){
            BatchDescribeTypeConfigurationsError batchDescribeTypeConfigurationsError = batchDescribeTypeConfigurationsResponse.errors().get(0);
            logger.log(String.format("Failed to Read the hook type configuration and the error is [%s]", batchDescribeTypeConfigurationsError.toString()));
            throw new CfnGeneralServiceException(batchDescribeTypeConfigurationsError.errorMessage());
        }
        final TypeConfigurationDetails typeConfig = batchDescribeTypeConfigurationsResponse.typeConfigurations().get(0);
        final String typeArn = typeConfig.arn()
                .substring(0,typeConfig.arn().lastIndexOf('/'))
                .replaceFirst("type-configuration","type");
        final String typeName = typeArn.substring(typeArn.lastIndexOf("/") + 1)
                .replace("-", "::");
        return ResourceModel.builder()
                .typeName(typeName)
                .configurationArn(typeConfig.arn())
                .typeArn(typeArn)
                .configurationAlias(typeConfig.alias())
                .configuration(typeConfig.configuration())
                .build();
    }

    static List<ResourceModel> translateFromListResponse(@NonNull final BatchDescribeTypeConfigurationsResponse batchDescribeTypeConfigurationsResponse, @NonNull final Logger logger) {
        if (batchDescribeTypeConfigurationsResponse.errors().size() > 0){
            BatchDescribeTypeConfigurationsError batchDescribeTypeConfigurationsError = batchDescribeTypeConfigurationsResponse.errors().get(0);
            logger.log(String.format("Failed to List the hook type configuration and the error is [%s]", batchDescribeTypeConfigurationsError.toString()));
            throw new CfnGeneralServiceException(batchDescribeTypeConfigurationsError.errorMessage());
        }
        return streamOfOrEmpty(batchDescribeTypeConfigurationsResponse.typeConfigurations())
                .map(typeConfig -> ResourceModel.builder()
                        .typeArn(typeConfig.arn()
                                .substring(0,typeConfig.arn().lastIndexOf('/'))
                                .replaceFirst("type-configuration","type"))
                        .typeName(typeConfig.arn().split("/")[2].replace("-", "::"))
                        .configurationAlias(typeConfig.alias())
                        .configuration(typeConfig.configuration())
                        .configurationArn(typeConfig.arn())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static BatchDescribeTypeConfigurationsRequest translateToListRequest(@NonNull ResourceModel resourceModel) {
        if (StringUtils.isNullOrEmpty(resourceModel.getTypeArn())) {
            return BatchDescribeTypeConfigurationsRequest.builder()
                    .typeConfigurationIdentifiers(TypeConfigurationIdentifier.builder()
                            .typeName(resourceModel.getTypeName())
                            .type(HOOK)
                            .build())
                    .build();
        }
        else {
            return BatchDescribeTypeConfigurationsRequest.builder()
                    .typeConfigurationIdentifiers(TypeConfigurationIdentifier.builder()
                            .typeArn(resourceModel.getTypeArn())
                            .build())
                    .build();
        }
    }

    /**
     * Delete handler will set TargetStacks to NONE while keeping all other properties in the config.
     */
    static SetTypeConfigurationRequest translateToDeleteRequest(@NonNull final ResourceModel model) {
        JSONObject disablingConfiguration = new JSONObject(model.getConfiguration());
        disablingConfiguration.getJSONObject("CloudFormationConfiguration")
                .getJSONObject("HookConfiguration")
                .put("TargetStacks", "NONE");
        if (!StringUtils.isNullOrEmpty(model.getTypeArn())){
            return SetTypeConfigurationRequest.builder()
                    .typeArn(model.getTypeArn())
                    .configurationAlias(model.getConfigurationAlias())
                    .configuration(disablingConfiguration.toString())
                    .build();
        }
        else {
            return SetTypeConfigurationRequest.builder()
                    .type(HOOK)
                    .configurationAlias(model.getConfigurationAlias())
                    .typeName(model.getTypeName())
                    .configuration(disablingConfiguration.toString())
                    .build();
        }
    }
}
