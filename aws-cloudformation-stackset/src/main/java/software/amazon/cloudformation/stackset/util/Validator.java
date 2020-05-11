package software.amazon.cloudformation.stackset.util;

import com.amazonaws.services.s3.AmazonS3URI;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.getObjectRequest;
import static software.amazon.cloudformation.stackset.util.TemplateParser.deserializeTemplate;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getMapFromTemplate;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getStringFromTemplate;

/**
 * Utility class to validate properties in {@link software.amazon.cloudformation.stackset.ResourceModel}
 */
public class Validator {

    // A stack name can contain only alphanumeric characters (case-sensitive) and hyphens.
    // It must start with an alphabetic character and can't be longer than 128 characters.
    private static final Pattern STACKSET_NAME_FORMAT = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\-]{0,127}$");

    private static final String TEMPLATE_RESOURCE_TYPE_KEY = "Type";
    private static final String TEMPLATE_RESOURCES_KEY = "Resources";
    private static final String TEMPLATE_PARAMETERS_KEY = "Parameters";


    /**
     * Validates the template to make sure:
     * <ul>
     *     <li> Template can be deserialized successfully
     *     <li> Resources block doesn't have embedded Stack or StackSet
     *     <li> Parameters block doesn't have embedded Stack or StackSet
     * </ul>
     *
     * @param content Template content
     */
    private static void validateTemplate(final String content) {
        final Map<String, Object> template = deserializeTemplate(content);
        validateBlocks(template, TEMPLATE_RESOURCES_KEY);
        validateBlocks(template, TEMPLATE_PARAMETERS_KEY);
    }

    /**
     * Validates items in the block do not have any invalid resources
     *
     * @param templateMap Templates map
     * @param block       Block key, i.e. Resources
     */
    @SuppressWarnings("unchecked")
    private static void validateBlocks(final Map<String, Object> templateMap, final String block) {
        final Map<String, Object> resourcesMap = (Map<String, Object>) templateMap.get(block);

        if (CollectionUtils.isNullOrEmpty(resourcesMap)) return;
        for (final Map.Entry<String, Object> entry : resourcesMap.entrySet()) {
            final String resourceId = entry.getKey();
            final Map<String, Object> resourceMap = getMapFromTemplate(resourcesMap, resourceId);
            validateResource(resourceMap);
        }
    }

    /**
     * Embedded Stack or StackSet is not allowed
     *
     * @param resourceMap Resource map
     */
    private static void validateResource(final Map<String, Object> resourceMap) {
        final String type = getStringFromTemplate(resourceMap.get(TEMPLATE_RESOURCE_TYPE_KEY));
        if (type != null) {
            switch (type) {
                case "AWS::CloudFormation::Stack":
                case "AWS::CloudFormation::StackSet":
                    throw new CfnInvalidRequestException(String.format("Nested %s is not allowed", type));
            }
        }
    }

    /**
     * Gets template content from s3 bucket
     *
     * @param proxy            {@link AmazonWebServicesClientProxy}
     * @param templateLocation Template URL
     * @return Template content from S3 object
     */
    @VisibleForTesting
    protected String getUrlContent(final AmazonWebServicesClientProxy proxy, final String templateLocation) {
        final AmazonS3URI s3Uri = new AmazonS3URI(templateLocation, true);
        final GetObjectRequest request = getObjectRequest(s3Uri.getBucket(), s3Uri.getKey());

        final String content = proxy.injectCredentialsAndInvokeV2Bytes(request,
                ClientBuilder.getS3Client()::getObjectAsBytes).asString(StandardCharsets.UTF_8);

        return content;
    }

    /**
     * Validates template url is valid S3 URL
     *
     * @param s3Uri Template URL
     */
    @VisibleForTesting
    protected void validateS3Uri(final String s3Uri) {
        try {
            final AmazonS3URI validS3Uri = new AmazonS3URI(s3Uri, true);
            if (Strings.isNullOrEmpty(validS3Uri.getBucket()) || Strings.isNullOrEmpty(validS3Uri.getKey())) {
                throw new CfnInvalidRequestException("Both S3 bucket and key must be specified");
            }
        } catch (final IllegalArgumentException | IllegalStateException | StringIndexOutOfBoundsException e) {
            throw new CfnInvalidRequestException("S3 URL is not valid");
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
     * @param proxy            {@link AmazonWebServicesClientProxy}
     * @param templateBody     {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateBody}
     * @param templateLocation {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateURL}
     * @param logger           {@link Logger}
     * @throws CfnInvalidRequestException if template is not valid
     */
    public void validateTemplate(
            final AmazonWebServicesClientProxy proxy,
            final String templateBody,
            final String templateLocation,
            final Logger logger) {

        if (Strings.isNullOrEmpty(templateBody) == Strings.isNullOrEmpty(templateLocation)) {
            throw new CfnInvalidRequestException("Exactly one of TemplateBody or TemplateUrl must be specified");
        }
        String content = null;
        try {
            if (!Strings.isNullOrEmpty(templateLocation)) {
                validateS3Uri(templateLocation);
                content = getUrlContent(proxy, templateLocation);
            } else {
                content = templateBody;
            }
            validateTemplate(content);

        } catch (final ParseException e) {
            logger.log(String.format("Failed to parse template content: %s", content));
            throw new CfnInvalidRequestException(e.getMessage());
        }
    }

    /**
     * Validates StackSetName and CfnInvalidRequestException will be thrown if StackSetName does not meet:
     * <ul>
     *    <li> Contains only alphanumeric characters (case-sensitive) and hyphens.
     *    <li> Starts with an alphabetic character
     *    <li> Length is no longer than 128 characters
     * </ul>
     * https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_CreateStackSet.html
     * @param stackSetName
     */
    public void validateStackSetName(final String stackSetName) {
        if (!STACKSET_NAME_FORMAT.matcher(stackSetName).matches()) {
            throw new CfnInvalidRequestException(
                    "A stack name can contain only alphanumeric characters (case-sensitive) and hyphens. " +
                            "It must start with an alphabetic character and can't be longer than 128 characters.");
        }
    }
}
