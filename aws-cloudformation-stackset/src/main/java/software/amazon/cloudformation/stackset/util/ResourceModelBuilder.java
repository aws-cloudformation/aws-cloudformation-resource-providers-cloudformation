package software.amazon.cloudformation.stackset.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.awssdk.services.cloudformation.model.StackInstanceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.HashSet;

import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.listStackInstancesRequest;

/**
 * Utility class to construct {@link ResourceModel} for Read/List request based on {@link StackSet}
 * that handler has retrieved.
 */
@AllArgsConstructor
@Builder
public class ResourceModelBuilder {

    private AmazonWebServicesClientProxy proxy;
    private CloudFormationClient client;
    private StackSet stackSet;
    private PermissionModels permissionModel;

    /**
     * Returns the model we construct from StackSet service client using PrimaryIdentifier StackSetId
     * @return {@link ResourceModel}
     */
    public ResourceModel buildModel() {
        permissionModel = stackSet.permissionModel();

        final String stackSetId = stackSet.stackSetId();

        // NOTE: TemplateURL from StackSet service client is currently not retrievable
        final ResourceModel model = ResourceModel.builder()
                .autoDeployment(translateFromSdkAutoDeployment(stackSet.autoDeployment()))
                .stackSetId(stackSetId)
                .description(stackSet.description())
                .permissionModel(stackSet.permissionModelAsString())
                .capabilities(new HashSet<>(stackSet.capabilitiesAsStrings()))
                .tags(translateFromSdkTags(stackSet.tags()))
                .regions(new HashSet<>())
                .parameters(translateFromSdkParameters(stackSet.parameters()))
                .templateBody(stackSet.templateBody())
                .deploymentTargets(DeploymentTargets.builder().build())
                .build();

        if (PermissionModels.SELF_MANAGED.equals(permissionModel)) {
            model.setAdministrationRoleARN(stackSet.administrationRoleARN());
            model.setExecutionRoleName(stackSet.executionRoleName());
        }

        String token = null;
        // Retrieves all Stack Instances associated with the StackSet,
        // Attaches regions and deploymentTargets to the constructing model
        do {
            putRegionsAndDeploymentTargets(stackSetId, model, token);
        } while (token != null);

        return model;
    }

    /**
     * Loop through all stack instance details and attach to the constructing model
     * @param stackSetId {@link ResourceModel#getStackSetId()}
     * @param model {@link ResourceModel}
     * @param token {@link ListStackInstancesResponse#nextToken()}
     */
    private void putRegionsAndDeploymentTargets(
            final String stackSetId,
            final ResourceModel model,
            String token) {

        final ListStackInstancesResponse listStackInstancesResponse = proxy.injectCredentialsAndInvokeV2(
                listStackInstancesRequest(token, stackSetId), client::listStackInstances);
        token = listStackInstancesResponse.nextToken();
        listStackInstancesResponse.summaries().forEach(member -> putRegionsAndDeploymentTargets(member, model));
    }

    /**
     * Helper method to attach StackInstance details to the constructing model
     * @param instance {@link StackInstanceSummary}
     * @param model {@link ResourceModel}
     */
    private void putRegionsAndDeploymentTargets(final StackInstanceSummary instance, final ResourceModel model) {
        model.getRegions().add(instance.region());

        if (model.getRegions() == null) model.setRegions(new HashSet<>());

        // If using SELF_MANAGED, getting accounts
        if (PermissionModels.SELF_MANAGED.equals(permissionModel)) {
            if (model.getDeploymentTargets().getAccounts() == null) {
                model.getDeploymentTargets().setAccounts(new HashSet<>());
            }
            model.getDeploymentTargets().getAccounts().add(instance.account());

        } else if (PermissionModels.SERVICE_MANAGED.equals(permissionModel)) {
            // If using SERVICE_MANAGED, getting OUIds
            if (model.getDeploymentTargets().getOrganizationalUnitIds() == null) {
                model.getDeploymentTargets().setOrganizationalUnitIds(new HashSet<>());
            }
            model.getDeploymentTargets().getOrganizationalUnitIds().add(instance.organizationalUnitId());

        } else {
            throw new CfnServiceInternalErrorException(
                    String.format("%s is not valid PermissionModels", permissionModel));
        }
    }
}
