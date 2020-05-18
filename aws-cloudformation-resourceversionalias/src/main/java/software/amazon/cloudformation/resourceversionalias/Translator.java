package software.amazon.cloudformation.resourceversionalias;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.cloudformation.proxy.Logger;

public class Translator {

    static SetTypeDefaultVersionRequest translateToUpdateRequest(@NonNull final ResourceModel model,
                                                                 @NonNull final Logger logger) {
        if (model.getArn() != null) {
            logger.log("Setting default version to: " + model.getArn());
            return SetTypeDefaultVersionRequest.builder()
                .arn(model.getArn())
                //.versionId(model.getDefaultVersionId())
                .build();
        } else {
            logger.log("Setting default version to: " + model.getTypeName() + ", " + model.getDefaultVersionId());
            return SetTypeDefaultVersionRequest.builder()
                .type(RegistryType.RESOURCE)
                .typeName(model.getTypeName())
                .versionId(model.getDefaultVersionId())
                .build();
        }
    }

    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        return DescribeTypeRequest.builder()
            .type(RegistryType.RESOURCE)
            .typeName(model.getTypeName())
            .versionId(model.getDefaultVersionId())
            .build();
    }

    static ResourceModel translateForRead(@NonNull final DescribeTypeResponse response) {
        return ResourceModel.builder()
            .arn(response.arn())
            .defaultVersionId(response.defaultVersionId())
            .typeName(response.typeName())
            .build();
    }
}
