package software.amazon.cloudformation.resourcedefaultversion;

import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

public class Translator {

    static SetTypeDefaultVersionRequest translateToUpdateRequest(@NonNull final ResourceModel model) {
        if (model.getTypeVersionArn() != null) {
            return SetTypeDefaultVersionRequest.builder()
                    .arn(model.getTypeVersionArn())
                    .build();
        } else {
            return SetTypeDefaultVersionRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(model.getTypeName())
                    .versionId(model.getVersionId())
                    .build();
        }
    }

    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        if (model.getTypeVersionArn() != null) {
            return DescribeTypeRequest.builder()
                    .arn(model.getTypeVersionArn())
                    .build();
        } else {
            return DescribeTypeRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(model.getTypeName())
                    .versionId(model.getVersionId())
                    .build();
        }
    }

    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse awsResponse) {
        return ResourceModel.builder()
                .typeVersionArn(awsResponse.arn())
                .versionId(awsResponse.defaultVersionId())
                .arn(awsResponse.arn().substring(0, awsResponse.arn().lastIndexOf("/")))
                .typeName(awsResponse.typeName())
                .build();
    }
}
