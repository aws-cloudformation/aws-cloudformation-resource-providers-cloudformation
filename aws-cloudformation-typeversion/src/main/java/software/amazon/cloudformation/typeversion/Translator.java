package software.amazon.cloudformation.typeversion;

import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

public class Translator {

    static SetTypeDefaultVersionRequest translateToUpdateRequest(final ResourceModel model) {
        if (model.getArn() != null) {
            return SetTypeDefaultVersionRequest.builder()
                .arn(model.getArn())
                .versionId(model.getDefaultVersionId())
                .build();
        } else {
            return SetTypeDefaultVersionRequest.builder()
                .type(model.getType())
                .typeName(model.getTypeName())
                .versionId(model.getDefaultVersionId())
                .build();
        }
    }

    static DescribeTypeRequest translateToReadRequest(final ResourceModel model) {
        return DescribeTypeRequest.builder()
            .type(model.getType())
            .typeName(model.getTypeName())
            .versionId(model.getDefaultVersionId())
            .build();
    }

    static ResourceModel translateForRead(final DescribeTypeResponse response) {
        return ResourceModel.builder()
            .arn(response.arn())
            .defaultVersionId(response.defaultVersionId())
            .type(response.type().toString())
            .typeName(response.typeName())
            .build();
    }
}
