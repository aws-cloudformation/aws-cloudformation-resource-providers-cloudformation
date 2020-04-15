package software.amazon.cloudformation.stackset.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to hold regions and targets that need to be modified during the update
 */
@Data
public class UpdatePlaceholder {

    @JsonProperty("RegionsToAdd")
    private Set<String> regionsToAdd;

    @JsonProperty("TargetsToAdd")
    private Set<String> targetsToAdd;

    @JsonProperty("RegionsToDelete")
    private Set<String> regionsToDelete;

    @JsonProperty("TargetsToDelete")
    private Set<String> targetsToDelete;

    /**
     * Analyzes regions and targets that need to be modified during the update
     * @param previousModel Previous {@link ResourceModel}
     * @param desiredModel Desired {@link ResourceModel}
     */
    public UpdatePlaceholder(final ResourceModel previousModel, final ResourceModel desiredModel) {
        final Set<String> previousRegions = previousModel.getRegions();
        final Set<String> desiredRegion = desiredModel.getRegions();

        Set<String> previousTargets;
        Set<String> desiredTargets;

        if (PermissionModels.SELF_MANAGED.equals(PermissionModels.fromValue(desiredModel.getPermissionModel()))) {
            previousTargets = previousModel.getDeploymentTargets().getAccounts();
            desiredTargets = desiredModel.getDeploymentTargets().getAccounts();
        } else {
            previousTargets = previousModel.getDeploymentTargets().getOrganizationalUnitIds();
            desiredTargets = desiredModel.getDeploymentTargets().getOrganizationalUnitIds();
        }

        // Calculates all necessary differences that we need to take actions
        regionsToAdd = new HashSet<>(desiredRegion);
        regionsToAdd.removeAll(previousRegions);
        targetsToAdd = new HashSet<>(desiredTargets);
        targetsToAdd.removeAll(previousTargets);

        regionsToDelete = new HashSet<>(previousRegions);
        regionsToDelete.removeAll(desiredRegion);
        targetsToDelete = new HashSet<>(previousTargets);
        targetsToDelete.removeAll(desiredTargets);

    }

}
