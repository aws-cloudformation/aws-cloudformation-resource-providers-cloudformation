package software.amazon.cloudformation.resourcedefaultversion;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

public class Translator {

    /**
     * Request to update a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static SetTypeDefaultVersionRequest translateToUpdateRequest(@NonNull final ResourceModel model) {
        if (model.getArn() != null) {
            return SetTypeDefaultVersionRequest.builder()
                .arn(model.getArn())
                .build();
        } else {
            return SetTypeDefaultVersionRequest.builder()
                .type(RegistryType.RESOURCE)
                .typeName(model.getTypeName())
                .versionId(model.getVersionId())
                .build();
        }
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static DescribeTypeRequest translateToReadRequest(@NonNull final ResourceModel model) {
        if (model.getArn() != null) {
            return DescribeTypeRequest.builder()
                .arn(model.getArn())
                .build();
        } else {
            return DescribeTypeRequest.builder()
                .type(RegistryType.RESOURCE)
                .typeName(model.getTypeName())
                .versionId(model.getVersionId())
                .build();
        }
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(@NonNull final DescribeTypeResponse awsResponse) {
        return ResourceModel.builder()
            .arn(awsResponse.arn())
            .versionId(awsResponse.defaultVersionId())
            .typeArn(StringUtils.isNullOrEmpty(awsResponse.arn())? null :awsResponse.arn().substring(0,awsResponse.arn().lastIndexOf("/")))
            .typeName(awsResponse.typeName())
            .build();
    }
}
