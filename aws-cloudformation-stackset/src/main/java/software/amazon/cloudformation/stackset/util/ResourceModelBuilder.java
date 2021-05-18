package software.amazon.cloudformation.stackset.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToStackInstance;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.listStackInstancesRequest;
import static software.amazon.cloudformation.stackset.util.InstancesAnalyzer.aggregateStackInstances;

/**
 * Utility class to construct {@link ResourceModel} for Read/List request based on {@link StackSet}
 * that handler has retrieved.
 */
@AllArgsConstructor
@Builder
public class ResourceModelBuilder {

    private ProxyClient<CloudFormationClient> proxyClient;
    private StackSet stackSet;
    private boolean isSelfManaged;

    public ResourceModel buildModel() {
        return buildModel(null);
    }
    /**
     * Returns the model we construct from StackSet service client using PrimaryIdentifier StackSetId
     *
     * @return {@link ResourceModel}
     */
    public ResourceModel buildModel(final String callAs) {

        final String stackSetId = stackSet.stackSetId();

        // NOTE: TemplateURL from StackSet service client is currently not retrievable
        final ResourceModel model = ResourceModel.builder()
                .stackSetName(stackSet.stackSetName())
                .autoDeployment(translateFromSdkAutoDeployment(stackSet.autoDeployment()))
                .stackSetId(stackSetId)
                .description(stackSet.description())
                .permissionModel(stackSet.permissionModelAsString() == null
                        ? PermissionModels.SELF_MANAGED.toString() : stackSet.permissionModelAsString())
                .capabilities(stackSet.hasCapabilities() ? new HashSet<>(stackSet.capabilitiesAsStrings()) : null)
                .tags(translateFromSdkTags(stackSet.tags()))
                .parameters(translateFromSdkParameters(stackSet.parameters()))
                .templateBody(stackSet.templateBody())
                .callAs(callAs)
                .build();

        isSelfManaged = Comparator.isSelfManaged(model);

        if (isSelfManaged) {
            model.setAdministrationRoleARN(stackSet.administrationRoleARN());
            model.setExecutionRoleName(stackSet.executionRoleName());
        }

        String token = null;
        final Set<StackInstance> stackInstanceSet = new HashSet<>();
        // Retrieves all Stack Instances associated with the StackSet,
        // Attaches regions and deploymentTargets to the constructing model
        do {
            token = attachStackInstances(stackSetId, isSelfManaged, stackInstanceSet, callAs, token);
        } while (token != null);

        if (!stackInstanceSet.isEmpty()) {
            final Set<StackInstances> stackInstancesGroup = aggregateStackInstances(stackInstanceSet, isSelfManaged);
            model.setStackInstancesGroup(stackInstancesGroup);
        }

        return model;
    }

    /**
     * Loop through all stack instance details and attach to the constructing model
     *
     * @param stackSetId    {@link ResourceModel#getStackSetId()}
     * @param isSelfManaged if permission model is SELF_MANAGED
     * @param token         {@link ListStackInstancesResponse#nextToken()}
     * @return String nextToken
     */
    private String attachStackInstances(
            final String stackSetId,
            final boolean isSelfManaged,
            final Set<StackInstance> stackInstanceSet,
            final String callAs,
            String token) {

        final ListStackInstancesResponse listStackInstancesResponse = proxyClient.injectCredentialsAndInvokeV2(
                listStackInstancesRequest(token, stackSetId, callAs), proxyClient.client()::listStackInstances);
        final String nextToken = listStackInstancesResponse.nextToken();
        if (!listStackInstancesResponse.hasSummaries()) return null;
        listStackInstancesResponse.summaries().forEach(member -> {
            // Parameters are set null as we can't retrieve parameter override from List API.
            // Retrieving from Describe API requires to brutal force every single stack instance
            // which will likely cause timeout issue
            stackInstanceSet.add(translateToStackInstance(isSelfManaged, member, null));
        });
        return nextToken;
    }
}
