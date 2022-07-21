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

    /**
     * Embedded Stack or StackSet is not allowed
     *
     * @param type Resource type
     */
    private static void validateResource(final String type) {
        switch (type) {
            case "AWS::CloudFormation::Stack":
            case "AWS::CloudFormation::StackSet":
                throw new CfnInvalidRequestException(
                        String.format("Nested %s is not supported in AWS::CloudFormation::StackSet", type));
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
            final String templateLocation) {

        final GetTemplateSummaryResponse response = proxyClient.injectCredentialsAndInvokeV2(
                getTemplateSummaryRequest(templateBody, templateLocation),
                proxyClient.client()::getTemplateSummary);

        if (response.hasResourceTypes()) {
            response.resourceTypes().forEach(Validator::validateResource);
        }
    }

    public static void validateServiceMangedInstancesGroup (final Collection<StackInstances> stackInstancesGroup) {
        if (CollectionUtils.isNullOrEmpty(stackInstancesGroup)) return;

        stackInstancesGroup.forEach(it ->
                validateServiceManagedDeploymentTarget(it.getDeploymentTargets())
        );
    }

    public static void validateServiceManagedDeploymentTarget (DeploymentTargets targets) {

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

        if (Objects.equals(filter, NONE) && !CollectionUtils.isNullOrEmpty(accounts)) {
            throw new CfnInvalidRequestException("AccountFilterType should be specified when both OrganizationalUnitIds and Accounts are provided");
        }

        if (!Objects.equals(filter, NONE) && CollectionUtils.isNullOrEmpty(accounts)) {
            throw new CfnInvalidRequestException("Accounts should be specified when Account-level Targeting is enabled");
        }
    }
}
