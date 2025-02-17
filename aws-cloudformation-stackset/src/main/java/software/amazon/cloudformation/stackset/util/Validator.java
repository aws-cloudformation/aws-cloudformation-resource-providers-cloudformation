package software.amazon.cloudformation.stackset.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.StackInstances;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.getTemplateSummaryRequest;

/**
 * Utility class to validate properties in {@link software.amazon.cloudformation.stackset.ResourceModel}
 */
public class Validator {

    private final static String INTER = "INTERSECTION";
    private final static String DIFF = "DIFFERENCE";
    private final static String NONE = "NONE";
    private final static HashSet<String> validAccountFilters = new HashSet<>(Arrays.asList(INTER, DIFF, NONE));
    private final static String STACK_RESOURCE = "AWS::CloudFormation::Stack";
    private final static String STACK_SET_RESOURCE = "AWS::CloudFormation::StackSet";


    /**
     * Embedded Stack or StackSet is not allowed
     *
     * @param type Resource type
     */
    private static void validateResource(final String type, boolean isSelfManaged) {

        switch (type) {
            case "AWS::CloudFormation::Stack":
                if(!isSelfManaged){
                    throw new CfnInvalidRequestException(
                            String.format("Nested %s is not supported in service managed %s", STACK_RESOURCE, STACK_SET_RESOURCE));
                }
                break;
            case "AWS::CloudFormation::StackSet":
                throw new CfnInvalidRequestException(
                        String.format("Nested %s is not supported in %s", STACK_SET_RESOURCE, STACK_SET_RESOURCE));
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
     * @param proxyClient      {@link ProxyClient <CloudFormationClient>}
     * @param templateBody     {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateBody}
     * @param templateLocation {@link software.amazon.cloudformation.stackset.ResourceModel#getTemplateURL}
     * @throws CfnInvalidRequestException if template is not valid
     */
    public void validateTemplate(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String templateBody,
            final String templateLocation,
            final boolean isSelfManaged) {

        final GetTemplateSummaryResponse response = proxyClient.injectCredentialsAndInvokeV2(
                getTemplateSummaryRequest(templateBody, templateLocation),
                proxyClient.client()::getTemplateSummary);

        if (response.hasResourceTypes()) {
            response.resourceTypes().forEach(resource -> Validator.validateResource(resource, isSelfManaged));
        }
    }

    public static void validateServiceMangedInstancesGroup (final Collection<StackInstances> stackInstancesGroup, boolean isAlt) {
        if (CollectionUtils.isNullOrEmpty(stackInstancesGroup)) return;

        stackInstancesGroup.forEach(it ->
                validateServiceManagedDeploymentTarget(it.getDeploymentTargets(), isAlt)
        );
    }

    public static void validateServiceManagedDeploymentTarget (DeploymentTargets targets, boolean isAlt) {

        if (targets == null) {
            throw new CfnInvalidRequestException("DeploymentTargets should be specified");
        }

        final Set<String> ous = targets.getOrganizationalUnitIds();

        if (CollectionUtils.isNullOrEmpty(ous)) {
            throw new CfnInvalidRequestException("OrganizationalUnitIds should be specified in SERVICE_MANAGED mode");
        }

        final String filter = targets.getAccountFilterType() == null?
                NONE: targets.getAccountFilterType();

        if (!validAccountFilters.contains(filter)) {
            throw new CfnInvalidRequestException(String.format("%s is not a valid AccountFilterType", filter));
        }

        final Set<String> accounts = CollectionUtils.isNullOrEmpty(targets.getAccounts()) ?
                new HashSet<>() : targets.getAccounts();

        if (isAlt && (Objects.equals(filter, NONE) && !CollectionUtils.isNullOrEmpty(accounts))) {
            throw new CfnInvalidRequestException("AccountFilterType should be specified when both OrganizationalUnitIds and Accounts are provided");
        }

        if (!Objects.equals(filter, NONE) && CollectionUtils.isNullOrEmpty(accounts)) {
            throw new CfnInvalidRequestException("Accounts should be specified when Account-level Targeting is enabled");
        }
    }
}
