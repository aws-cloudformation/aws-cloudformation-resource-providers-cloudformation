package software.amazon.cloudformation.resourcedefaultversion;

import com.amazonaws.util.StringUtils;
import lombok.NonNull;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    static List<ResourceModel> translateFromListResponse(@NonNull final ListTypeVersionsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.typeVersionSummaries())
                .map(typeSummary -> ResourceModel.builder()
                        .arn(typeSummary.arn().substring(0, typeSummary.arn().lastIndexOf("/")))
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static ListTypeVersionsRequest translateToListRequest(ResourceModel resourceModel, final String nextToken) {

        if (StringUtils.isNullOrEmpty(resourceModel.getArn())) {
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
                    .arn(resourceModel.getArn())
                    .build();
        }
    }
}
