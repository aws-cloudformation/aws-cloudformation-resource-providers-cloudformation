package software.amazon.cloudformation.stackset.util;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.ProxyClient;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.getTemplateSummaryRequest;

/**
 * Utility class to validate properties in {@link software.amazon.cloudformation.stackset.ResourceModel}
 */
public class Validator {

    /**
     * Embedded Stack or StackSet is not allowed
     *
     * @param type Resource type
     */
    private static void validateResource(final String type) {
        switch (type) {
            case "AWS::CloudFormation::Stack":
            case "AWS::CloudFormation::StackSet":
                throw new CfnInvalidRequestException(String.format("Nested %s is not allowed", type));
        }
    }

    /**
     * Validates template with following rules:
     * <ul>
     *     <li> Only exact one template source can be specified
     *     <li> If using S3 URI, it must be valid
     *     <li> Template contents must be valid
     * </ul>
     *
     * @param proxyClient      {@link ProxyClient < CloudFormationClient >}
     * @param templateBody     {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateBody}
     * @param templateLocation {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateURL}
     * @throws CfnInvalidRequestException if template is not valid
     */
    public void validateTemplate(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String templateBody,
            final String templateLocation) {

        final GetTemplateSummaryResponse response = proxyClient.injectCredentialsAndInvokeV2(
                getTemplateSummaryRequest(templateBody, templateLocation),
                proxyClient.client()::getTemplateSummary);
        if (response.hasResourceTypes()) {
            response.resourceTypes().forEach(Validator::validateResource);
        }
    }
}
