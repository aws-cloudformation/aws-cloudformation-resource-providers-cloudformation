package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateStackRequest translateToCreateRequest(final ResourceModel model) {
    return CreateStackRequest.builder()
        .notificationARNs(model.getNotificationARNs())
        .parameters(translateToSdkParameters(model))
        .tags(translateToSdkTags(model))
        .templateURL(model.getTemplateURL())
        .timeoutInMinutes(model.getTimeoutInMinutes())
        .stackName(model.getStackName())
        .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeStacksRequest translateToReadRequest(final ResourceModel model) {
    return DescribeStacksRequest.builder()
        .stackName(model.getArn() == null ? model.getStackName() : model.getArn())
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param stack stack returned from DescribeStacks response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final Stack stack) {
    final List<software.amazon.cloudformation.stackv2.Tag> tagsSystemTagsRemoved = stack.tags().stream()
        .filter(t -> !t.key().startsWith("aws:"))
        .map(t -> software.amazon.cloudformation.stackv2.Tag.builder().key(t.key()).value(t.value()).build())
        .collect(Collectors.toList());
    final Map<String, String> parameters = stack.parameters().stream()
        .collect(Collectors.toMap(p -> p.parameterKey(), p -> p.parameterValue()));
    return ResourceModel.builder()
        .arn(stack.stackId())
        .notificationARNs(stack.notificationARNs().isEmpty() ? null : new HashSet<>(stack.notificationARNs()))
        .parameters(parameters.isEmpty() ? null : parameters)
        .stackName(stack.stackName())
        .tags(tagsSystemTagsRemoved.isEmpty() ? null : tagsSystemTagsRemoved)
        .timeoutInMinutes(stack.timeoutInMinutes())
        .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteStackRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteStackRequest.builder()
        .stackName(model.getArn())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateStackRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateStackRequest.builder()
        .notificationARNs(model.getNotificationARNs())
        .parameters(translateToSdkParameters(model))
        .tags(translateToSdkTags(model))
        .templateURL(model.getTemplateURL())
        .stackName(model.getArn())
        .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListStacksRequest translateToListRequest(final String nextToken) {
    return ListStacksRequest.builder()
        .nextToken(nextToken)
        .build();
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  static List<Parameter> translateToSdkParameters(final ResourceModel model) {
    if (model.getParameters() == null) {
      return null;
    }
    return model.getParameters().entrySet().stream()
        .map(e -> Parameter.builder().parameterKey(e.getKey()).parameterValue(e.getValue()).build())
        .collect(Collectors.toList());
  }

  static List<Tag> translateToSdkTags(final ResourceModel model) {
    if (model.getTags() == null) {
      return null;
    }
    return model.getTags().stream()
        .map(t -> Tag.builder().key(t.getKey()).value(t.getValue()).build())
        .collect(Collectors.toList());
  }
}
