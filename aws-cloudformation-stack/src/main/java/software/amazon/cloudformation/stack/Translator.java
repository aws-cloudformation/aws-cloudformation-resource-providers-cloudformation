package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.model.CloudFormationRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;

import software.amazon.awssdk.services.cloudformation.model.ListStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


import java.util.ArrayList;
import java.util.Collection;
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
        .roleARN(model.getRoleARN())
        .capabilitiesWithStrings(model.getCapabilities())
        .templateBody(model.getTemplateBody())
        .stackPolicyBody(model.getStackPolicyBody())
        .stackPolicyURL(model.getStackPolicyURL())
        .disableRollback(model.getDisableRollback())
        .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeStacksRequest translateToReadRequest(final ResourceModel model) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return DescribeStacksRequest.builder()
        .stackName(model.getStackId())
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param stack the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final Stack stack) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    final List<software.amazon.cloudformation.stack.Tag> tagsSystemTagsRemoved = stack.tags().stream()
        .filter(t -> !t.key().startsWith("aws:"))
        .map(t -> software.amazon.cloudformation.stack.Tag.builder().key(t.key()).value(t.value()).build())
        .collect(Collectors.toList());
    final Map<String, String> parameters = stack.parameters().stream()
        .collect(Collectors.toMap(Parameter::parameterKey, Parameter::parameterValue));
    final List<software.amazon.cloudformation.stack.Output> outputs = stack.outputs().stream()
        .map(o -> software.amazon.cloudformation.stack.Output.builder()
            .outputKey(o.outputKey())
            .outputValue(o.outputValue())
            .description(o.description())
            .exportName(o.exportName())
            .build())
        .collect(Collectors.toList());
    return ResourceModel.builder()
        .description(stack.description())
        .creationTime(stack.creationTime().toString())
        .lastUpdateTime(stack.lastUpdatedTime() == null? null:stack.lastUpdatedTime().toString())
        .enableTerminationProtection(stack.enableTerminationProtection())
        .rootId(stack.rootId() == null? "None": stack.rootId())
        .parentId(stack.parentId() == null? "None" : stack.parentId())
        .stackId(stack.stackId())
        .stackStatus(stack.stackStatusAsString())
        .stackStatusReason(stack.stackStatusReason()==null? " ":stack.stackStatusReason())
        .notificationARNs(stack.notificationARNs().isEmpty() ? null : new ArrayList<>(stack.notificationARNs()))
        .parameters(parameters.isEmpty()? null : parameters)
        .stackName(stack.stackName())
        .tags(tagsSystemTagsRemoved.isEmpty() ? null : tagsSystemTagsRemoved)
        .timeoutInMinutes(stack.timeoutInMinutes())
        .roleARN(stack.roleARN())
        .capabilities(stack.capabilitiesAsStrings().isEmpty() ? null : stack.capabilitiesAsStrings())
        .outputs(outputs)
        .disableRollback(stack.disableRollback())
        .driftInformation(software.amazon.cloudformation.stack.StackDriftInformation.builder()
                .stackDriftStatus(stack.driftInformation().stackDriftStatusAsString())
                .lastCheckTimestamp(stack.driftInformation().lastCheckTimestamp() == null? "None": stack.driftInformation().lastCheckTimestamp().toString())
            .build())
        .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteStackRequest translateToDeleteRequest(final ResourceModel model) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37
    return DeleteStackRequest.builder()
        .stackName(model.getStackId())
        .build();
  }
  /**
   * Request to update a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static UpdateStackRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateStackRequest.builder()
        .notificationARNs(model.getNotificationARNs())
        .parameters(translateToSdkParameters(model))
        .tags(translateToSdkTags(model))
        .templateURL(model.getTemplateURL())
        .stackName(model.getStackId())
        .roleARN(model.getRoleARN())
        .capabilitiesWithStrings(model.getCapabilities())
        .templateBody(model.getTemplateBody())
        .stackPolicyBody(model.getStackPolicyBody())
        .stackPolicyURL(model.getStackPolicyURL())
        .disableRollback(model.getDisableRollback())
        .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListStacksRequest translateToListRequest(final String nextToken) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L26-L31
    return ListStacksRequest.builder()
        .nextToken(nextToken)
        .build();
  }

  /**
   * Request to list resources
   * @param response List reqsponse
   * @return List of ResourceModels
   */
  static  List<ResourceModel>translateFromListResponse(ListStacksResponse response){
    return response.stackSummaries().stream()
        .filter(stack -> stack.stackStatus() != StackStatus.DELETE_COMPLETE)
        .map(stack -> ResourceModel.builder()
            .stackId(stack.stackId())
            .parentId(stack.parentId())
            .rootId(stack.rootId()).build())
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

  static List<Parameter> translateToSdkParameters(final ResourceModel model) {
    if (model.getParameters() == null) {
      return null;
    }
    return model.getParameters().entrySet().stream()
        .map(e -> Parameter.builder().parameterKey(e.getKey()).parameterValue(e.getValue()).build())
        .collect(Collectors.toList());
  }

  static List<software.amazon.cloudformation.stack.Tag> mergeRequestTagWithModelTags(Map<String, String> requestTags, ResourceModel model) {
    List<software.amazon.cloudformation.stack.Tag> resourceTags = requestTags.entrySet()
        .stream()
        .map(e -> software.amazon.cloudformation.stack.Tag.builder().key(e.getKey()).value(e.getValue()).build())
        .collect(Collectors.toList());
    if(model.getTags()  != null) {
      resourceTags.addAll(model.getTags());
    }
    return resourceTags;
  }
}
