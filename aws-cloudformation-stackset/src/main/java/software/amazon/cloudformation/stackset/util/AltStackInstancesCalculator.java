package software.amazon.cloudformation.stackset.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.Parameter;
import software.amazon.cloudformation.stackset.StackInstances;
import static software.amazon.cloudformation.stackset.util.AltResourceModelAnalyzer.setDiff;
import static software.amazon.cloudformation.stackset.util.AltResourceModelAnalyzer.setInter;
import static software.amazon.cloudformation.stackset.util.AltResourceModelAnalyzer.setUnion;

@Data
public class AltStackInstancesCalculator {

    private final String region;

    private final Set<StackInstances> previousStackInstancesGroup;
    private final Set<StackInstances> currentStackInstancesGroup;

    private final Set<String> previousOUs;
    private final Set<String> currentOUs;

    private final HashMap<List<String>, Set<String>> previousStackInstancesByOuFilter;
    private final HashMap<List<String>, Set<String>> currentStackInstancesByOuFilter;
    private final HashMap<String, Set<Parameter>> ouDeploymentParametersMap;

    private final static String NONE = "NONE";
    private final static String INTER = "INTERSECTION";
    private final static String DIFF = "DIFFERENCE";

    /*
    * The class compares the StackInstances from previous and current models in one region.
    * Process steps:
    * 1. Process the StackInstancesGroup for previous and current models, separately:
    * 1.1 For each OU, merge all the targets with same ALT filters  (e.g., merge all the INTERs to one INTER) -- mergeSameFilters()
    * 1.2 For each OU, merge all the targets with different ALT filters (e.g., merge INTER and DIFF) -- mergeDifferentFilters()
    * 2. Compare the previous and current StackInstancesGroup to calculate the targets to delete, create, and delete
    * 2.1 the comparison is done by constructTargetSetForSharedOu(), which list all nine possible cases (three types of filter x three types of filter)
    * */

    public AltStackInstancesCalculator (String region, Set<StackInstances> previousStackInstancesGroup, Set<StackInstances> currentStackInstancesGroup) {
        this.region = region;
        this.previousStackInstancesGroup = previousStackInstancesGroup;
        this.currentStackInstancesGroup = currentStackInstancesGroup;
        this.previousOUs = findAllOus(previousStackInstancesGroup);
        this.currentOUs = findAllOus(currentStackInstancesGroup);
        this.ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesGroup);
        this.previousStackInstancesByOuFilter = mergeDifferentFilters(mergeSameFilters(previousStackInstancesGroup));
        this.currentStackInstancesByOuFilter = mergeDifferentFilters(mergeSameFilters(currentStackInstancesGroup));
    }

    public void calculate (Set<StackInstances> instancesToDelete, Set<StackInstances> instancesToCreate, Set<StackInstances> instancesToUpdate) {
        setDiff(previousOUs, currentOUs).forEach(ou -> {
            String filter = getFilterType(ou, previousStackInstancesByOuFilter);
            Set<String> accounts = previousStackInstancesByOuFilter.get(Arrays.asList(ou, filter));
            DeploymentTargets targets = constructDeploymentTargets(ou, accounts, filter);
            addToInstancesSet(targets, instancesToDelete);
        });

        setDiff(currentOUs, previousOUs).forEach(ou -> {
            String filter = getFilterType(ou, currentStackInstancesByOuFilter);
            Set<String> accounts = currentStackInstancesByOuFilter.get(Arrays.asList(ou, filter));
            DeploymentTargets targets = constructDeploymentTargets(ou, accounts, filter);
            Set<Parameter> parameters = ouDeploymentParametersMap.get(ou);
            addToInstancesSet(targets, instancesToCreate, parameters);
        });

        setInter(currentOUs, previousOUs).forEach(ou -> {
            Set<Parameter> parameters = ouDeploymentParametersMap.get(ou);
            Set<DeploymentTargets> deleteTargetSet = new HashSet<>();
            Set<DeploymentTargets> createTargetSet = new HashSet<>();
            Set<DeploymentTargets> updateTargetSet = new HashSet<>();
            constructTargetSetForSharedOu(ou, deleteTargetSet, createTargetSet, updateTargetSet);
            deleteTargetSet.forEach(targets -> addToInstancesSet(targets, instancesToDelete));
            createTargetSet.forEach(targets -> addToInstancesSet(targets, instancesToCreate, parameters));
            updateTargetSet.forEach(targets -> addToInstancesSet(targets, instancesToUpdate, parameters));
        });
    }

    /*
    *  If an OU is associated with different parameter sets, will raise an error.
    *  1. This is the original process logic when ALT is not enabled.
    *  2. Although users logically CAN associate an OU with different with ALT filter, but we cannot check if the input is valid
    *  2.1 For example, (OU - account1) and (OU - account2). It's up to OU's structure if these two targets can be
    *      associated with two parameters -- if OU is set(account1, account2, account3), then not valid.
    *  2.2 So we chose to raise and error to align with previous implementation and avoid possible ambiguity
    * */

    private static HashMap<String, Set<Parameter>> findDeploymentParametersForOUs(final Set<StackInstances> stackInstancesGroup) {
        HashMap<String, Set<Parameter>> ouDeploymentParameters = new HashMap<>();

        for (final StackInstances stackInstances : stackInstancesGroup) {
            Set<Parameter> parameters = stackInstances.getParameterOverrides();

            stackInstances.getDeploymentTargets().getOrganizationalUnitIds().forEach(
                    ou -> {
                        if (ouDeploymentParameters.containsKey(ou) && ouDeploymentParameters.get(ou) != parameters) {
                            throw new CfnInvalidRequestException("An OrganizationalUnitIds cannot be associated with more than one Parameters set");
                        }
                        ouDeploymentParameters.put(ou, parameters);
                    }
            );
        }
        return ouDeploymentParameters;
    }

    private static Set<String> findAllOus(final Set<StackInstances> stackInstancesGroup) {
        final HashSet<String> OUs = new HashSet<>();
        stackInstancesGroup.forEach(stackInstances -> {
            OUs.addAll(stackInstances.getDeploymentTargets().getOrganizationalUnitIds());
        });
        return OUs;
    }

    private static HashMap<List<String>, Set<String>> mergeSameFilters (final Set<StackInstances> stackInstancesGroup) {
        final HashMap<List<String>, Set<String>> ouFilterAccountsMap = new HashMap<>();

        stackInstancesGroup.forEach(stackInstances -> {
            DeploymentTargets deploymentTargets = stackInstances.getDeploymentTargets();
            final Set<String> ous = deploymentTargets.getOrganizationalUnitIds();
            final Set<String> accounts = CollectionUtils.isNullOrEmpty(deploymentTargets.getAccounts()) ?
                    new HashSet<>() : deploymentTargets.getAccounts();
            String filter = deploymentTargets.getAccountFilterType() == null ?
                    NONE : deploymentTargets.getAccountFilterType();

            for (final String ou : ous) {
                final List<String> compositeKey = Arrays.asList(ou, filter);
                if (!ouFilterAccountsMap.containsKey(compositeKey)) {
                    ouFilterAccountsMap.put(compositeKey, new HashSet<>(accounts));
                    continue;
                }
                /*
                 * NONE: only allows empty account, so no need to add repeatedly
                 * INTER: take the union of all account sets
                 * DIFF: take the intersection of all account sets
                 * */
                switch (filter) {
                    case NONE:
                        break;
                    case INTER:
                        ouFilterAccountsMap.get(compositeKey).addAll(accounts);
                        break;
                    case DIFF:
                        ouFilterAccountsMap.get(compositeKey).retainAll(accounts);
                        break;
                }
            }
        });
        return ouFilterAccountsMap;
    }

    private static HashMap<List<String>, Set<String>> mergeDifferentFilters (HashMap<List<String>, Set<String>> ouFilterAccountsMap) {
        final HashMap<List<String>, Set<String>> mergedOuFilterAccountsMap = new HashMap<>();
        final Set<String> ous = ouFilterAccountsMap.keySet()
                .stream()
                .map(it -> it.get(0))
                .collect(Collectors.toSet());

        for (final String ou : ous) {
            List<String> noneCompositeKey = Arrays.asList(ou, NONE);
            List<String> interCompositeKey = Arrays.asList(ou, INTER);
            List<String> diffCompositeKey = Arrays.asList(ou, DIFF);

            /*
             *  NONE: if a NONE filter appears, the OU itself is the deployment target
             *  INTER only : no change needed
             *  DIFF only : no change needed
             *  Both INTER and DIFF: equivalent to OU - (diffSet - interSet), where diffSet and interSet are account sets
             * */
            if (ouFilterAccountsMap.containsKey(noneCompositeKey)) {
                mergedOuFilterAccountsMap.put(noneCompositeKey, ouFilterAccountsMap.get(noneCompositeKey));
            } else if (ouFilterAccountsMap.containsKey(interCompositeKey) && !ouFilterAccountsMap.containsKey(diffCompositeKey)) {
                mergedOuFilterAccountsMap.put(interCompositeKey, ouFilterAccountsMap.get(interCompositeKey));
            } else if (!ouFilterAccountsMap.containsKey(interCompositeKey) && ouFilterAccountsMap.containsKey(diffCompositeKey)) {
                mergedOuFilterAccountsMap.put(diffCompositeKey, ouFilterAccountsMap.get(diffCompositeKey));
            } else {
                mergedOuFilterAccountsMap.put(diffCompositeKey,
                        setDiff(ouFilterAccountsMap.get(diffCompositeKey), ouFilterAccountsMap.get(interCompositeKey)));
            }
        }
        return mergedOuFilterAccountsMap;
    }

    private void constructTargetSetForSharedOu (final String ou,
                                                Set<DeploymentTargets> deleteTargetSet,
                                                Set<DeploymentTargets> createTargetSet,
                                                Set<DeploymentTargets> updateTargetSet) {

        final String previousFilter = getFilterType(ou, previousStackInstancesByOuFilter);
        final String currentFilter = getFilterType(ou, currentStackInstancesByOuFilter);
        final Set<String> previousAccountSet = previousStackInstancesByOuFilter.get(Arrays.asList(ou, previousFilter));
        final Set<String> currentAccountSet = currentStackInstancesByOuFilter.get(Arrays.asList(ou, currentFilter));

        switch (previousFilter) {
            case NONE:
                switch (currentFilter) {
                    case NONE:
                        updateTargetSet.add(constructDeploymentTargets(ou));
                        break;
                    case INTER:
                        updateTargetSet.add(constructDeploymentTargets(ou, currentAccountSet, INTER));
                        deleteTargetSet.add(constructDeploymentTargets(ou, currentAccountSet, DIFF));
                        break;
                    case DIFF:
                        updateTargetSet.add(constructDeploymentTargets(ou, currentAccountSet, DIFF));
                        deleteTargetSet.add(constructDeploymentTargets(ou, currentAccountSet, INTER));
                        break;
                }
                break;
            case INTER:
                switch (currentFilter) {
                    case NONE:
                        updateTargetSet.add(constructDeploymentTargets(ou, previousAccountSet, INTER));
                        createTargetSet.add(constructDeploymentTargets(ou, previousAccountSet, DIFF));
                        break;
                    case INTER:
                        updateTargetSet.add(constructDeploymentTargets(ou, setInter(previousAccountSet, currentAccountSet), INTER));
                        deleteTargetSet.add(constructDeploymentTargets(ou, setDiff(previousAccountSet, currentAccountSet), INTER));
                        createTargetSet.add(constructDeploymentTargets(ou, setDiff(currentAccountSet, previousAccountSet), INTER));
                        break;
                    case DIFF:
                        updateTargetSet.add(constructDeploymentTargets(ou, setDiff(previousAccountSet, currentAccountSet), INTER));
                        deleteTargetSet.add(constructDeploymentTargets(ou, setInter(previousAccountSet, currentAccountSet), INTER));
                        createTargetSet.add(constructDeploymentTargets(ou, setUnion(previousAccountSet, currentAccountSet), DIFF));
                        break;
                }
                break;
            case DIFF:
                switch (currentFilter) {
                    case NONE:
                        updateTargetSet.add(constructDeploymentTargets(ou, previousAccountSet, DIFF));
                        createTargetSet.add(constructDeploymentTargets(ou, previousAccountSet, INTER));
                        break;
                    case INTER:
                        updateTargetSet.add(constructDeploymentTargets(ou, setDiff(currentAccountSet, previousAccountSet), INTER));
                        deleteTargetSet.add(constructDeploymentTargets(ou, setUnion(previousAccountSet, currentAccountSet), DIFF));
                        createTargetSet.add(constructDeploymentTargets(ou, setInter(previousAccountSet, currentAccountSet), INTER));
                        break;
                    case DIFF:
                        updateTargetSet.add(constructDeploymentTargets(ou, setUnion(previousAccountSet, currentAccountSet), DIFF));
                        deleteTargetSet.add(constructDeploymentTargets(ou, setDiff(currentAccountSet, previousAccountSet), INTER));
                        createTargetSet.add(constructDeploymentTargets(ou, setDiff(previousAccountSet, currentAccountSet), INTER));
                        break;
                }
                break;
        }
    }

    private static String getFilterType(String ou, HashMap<List<String>, Set<String>> ouFilterAccountsMap) {

        List<String> noneCompositeKey = Arrays.asList(ou, NONE);
        List<String> interCompositeKey = Arrays.asList(ou, INTER);

        if (ouFilterAccountsMap.containsKey(noneCompositeKey)) {
            return NONE;
        } else if (ouFilterAccountsMap.containsKey(interCompositeKey)) {
            return INTER;
        } else {
            return DIFF;
        }
    }

    /*
     * When the account set is empty
     * 1. If filter is INTER, no instance should be deployed in OUs
     * 2. If filter is DIFF, all instances should be deployed in OUs
     * */
    private void addToInstancesSet(DeploymentTargets targets, Set<StackInstances> instancesSet) {
        String filter = targets.getAccountFilterType();
        Set<String> accounts = targets.getAccounts();
        if (CollectionUtils.isNullOrEmpty(accounts)) {
            if (filter.equals(INTER)) {
                return;
            } else if (filter.equals(DIFF)) {
                filter = NONE;
            }
        }

        instancesSet.add(constructStackInstances(region, DeploymentTargets.builder()
                .organizationalUnitIds(targets.getOrganizationalUnitIds())
                .accounts(targets.getAccounts())
                .accountFilterType(filter).build()));
    }

    private void addToInstancesSet(DeploymentTargets targets, Set<StackInstances> instancesSet, Set<Parameter> parameters) {
        String filter = targets.getAccountFilterType();
        Set<String> accounts = targets.getAccounts();
        if (CollectionUtils.isNullOrEmpty(accounts)) {
            if (filter.equals(INTER)) {
                return;
            } else if (filter.equals(DIFF)) {
                filter = NONE;
            }
        }

        instancesSet.add(constructStackInstances(
                        region,
                        DeploymentTargets.builder()
                        .organizationalUnitIds(targets.getOrganizationalUnitIds())
                        .accounts(targets.getAccounts())
                        .accountFilterType(filter).build(),
                parameters));
    }

    private static StackInstances constructStackInstances (String region, DeploymentTargets targets, Set<Parameter> parameters) {
        return StackInstances.builder()
                .regions(Collections.singleton(region))
                .deploymentTargets(targets)
                .parameterOverrides(parameters)
                .build();
    }

    private static StackInstances constructStackInstances (String region, DeploymentTargets targets) {
        return StackInstances.builder()
                .regions(Collections.singleton(region))
                .deploymentTargets(targets)
                .build();
    }

    private static DeploymentTargets constructDeploymentTargets (String ou) {
        return constructDeploymentTargets(ou, new HashSet<>(Collections.emptyList()), NONE);
    }

    private static DeploymentTargets constructDeploymentTargets (String ou, Set<String> accounts, String filter) {

        return DeploymentTargets
                .builder()
                .organizationalUnitIds(new HashSet<> (Collections.singletonList(ou)))
                .accounts(accounts)
                .accountFilterType(filter)
                .build();
    }
}
