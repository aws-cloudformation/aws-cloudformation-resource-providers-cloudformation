package software.amazon.cloudformation.stackset.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.Parameter;
import software.amazon.cloudformation.stackset.StackInstances;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.DIFF;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.INTER;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.NONE;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_4;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_5;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_4;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_5;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateDeleteInstances;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstances;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.parameters_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.parameters_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;

public class AltStackInstancesCalculatorTest {

    @Test
    public void test_No_Alt_Filter() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2)),
                generateInstances(Arrays.asList(OU_2, OU_3))));

        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_2, OU_3)),
                generateInstances(Arrays.asList(OU_3, OU_4))));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Collections.singletonList(
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Collections.singletonList(
                generateInstances(OU_4, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_2, Collections.emptyList(), NONE),
                generateInstances(OU_3, Collections.emptyList(), NONE)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_No_Alt_Filter_Multiple_Parameters_Across_Groups() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2), parameters_1),
                generateInstances(Arrays.asList(OU_2, OU_3), parameters_1)));

        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_2, OU_3), parameters_2),
                generateInstances(Arrays.asList(OU_3, OU_4), parameters_2)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Collections.singletonList(
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Collections.singletonList(
                generateInstances(OU_4, Collections.emptyList(), NONE, parameters_2)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_2, Collections.emptyList(), NONE, parameters_2),
                generateInstances(OU_3, Collections.emptyList(), NONE, parameters_2)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_None_Filters_Take_Over_For_OUs() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2), Collections.emptyList(), NONE),
                generateInstances(Arrays.asList(OU_2, OU_3), Collections.emptyList(), NONE),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), INTER),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_2, account_3), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_3, account_4), INTER)));
        Set<StackInstances> currentGroup = new HashSet<>(Collections.singletonList(generateInstances(OU_4)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE),
                generateDeleteInstances(OU_3, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Collections.singletonList(
                generateInstances(OU_4, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Merge_Inter_Filters_For_OUs() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), INTER),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_2, account_3), INTER),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_3, account_4), INTER)));
        Set<StackInstances> currentGroup = new HashSet<>(Collections.singletonList(generateInstances(OU_4)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE),
                generateDeleteInstances(OU_2, Arrays.asList(account_1, account_2, account_3, account_4), INTER),
                generateDeleteInstances(OU_3, Arrays.asList(account_2, account_3, account_4), INTER)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Collections.singletonList(
                generateInstances(OU_4, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);

        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Merge_Diff_Filters_For_OUs() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_2, account_3), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_2, OU_3), Arrays.asList(account_3, account_4), DIFF)));
        Set<StackInstances> currentGroup = new HashSet<>(Collections.singletonList(generateInstances(OU_4)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE),
                generateDeleteInstances(OU_3, account_3, DIFF)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Collections.singletonList(
                generateInstances(OU_4, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Merge_Inter_Diff_Filters_For_OUs() {
        Set<StackInstances> previousGroup = new HashSet<>(Collections.singletonList(generateInstances(OU_4)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), INTER),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_1, account_3), INTER),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_3, account_4), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_3, account_5), DIFF)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Collections.singletonList(
                generateDeleteInstances(OU_4, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Collections.emptyList(), NONE),
                generateInstances(OU_2, Arrays.asList(account_3, account_4), DIFF),
                generateInstances(OU_3, account_5, DIFF)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Update_Previous_no_Alt_Filter() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(OU_2),
                generateInstances(OU_3)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_3, Arrays.asList(account_3, account_4), DIFF)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_2, Arrays.asList(account_1, account_2), DIFF),
                generateDeleteInstances(OU_3, Arrays.asList(account_3, account_4), INTER)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>();
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Collections.emptyList(), NONE),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_3, Arrays.asList(account_3, account_4), DIFF)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Update_Previous_None_Filters() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Collections.emptyList(), NONE),
                generateInstances(OU_2, Collections.emptyList(), NONE),
                generateInstances(OU_3, Collections.emptyList(), NONE)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_3, Arrays.asList(account_1, account_2), DIFF)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_2, Arrays.asList(account_1, account_2), DIFF),
                generateDeleteInstances(OU_3, Arrays.asList(account_1, account_2), INTER)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>();
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Collections.emptyList(), NONE),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_3, Arrays.asList(account_1, account_2), DIFF)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Update_Previous_Inter_Filter() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_3, Arrays.asList(account_1, account_2), INTER)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(OU_2, Arrays.asList(account_2, account_3), INTER),
                generateInstances(OU_3, Arrays.asList(account_2, account_3), DIFF)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_2, account_1, INTER),
                generateDeleteInstances(OU_3, account_2, INTER)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), DIFF),
                generateInstances(OU_2, account_3, INTER),
                generateInstances(OU_3, Arrays.asList(account_1, account_2, account_3), DIFF)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_2, account_2, INTER),
                generateInstances(OU_3, account_1, INTER)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Update_Previous_Diff_Filter() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), DIFF),
                generateInstances(OU_2, Arrays.asList(account_1, account_2), DIFF),
                generateInstances(OU_3, Arrays.asList(account_1, account_2), DIFF)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(OU_1),
                generateInstances(OU_2, Arrays.asList(account_2, account_3), INTER),
                generateInstances(OU_3, Arrays.asList(account_2, account_3), DIFF)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_2, Arrays.asList(account_1, account_2, account_3), DIFF),
                generateDeleteInstances(OU_3, account_3, INTER)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), INTER),
                generateInstances(OU_2, account_2, INTER),
                generateInstances(OU_3, account_1, INTER)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, Arrays.asList(account_1, account_2), DIFF),
                generateInstances(OU_2, account_3, INTER),
                generateInstances(OU_3, Arrays.asList(account_1, account_2, account_3), DIFF)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_A_Unnecessarily_And_Unrealistically_Complicated_Case() {
        Set<StackInstances> previousGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_3), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_3, account_4), INTER),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_3, account_5), INTER),
                generateInstances(OU_4, Arrays.asList(account_3, account_4), DIFF)));
        Set<StackInstances> currentGroup = new HashSet<>(Arrays.asList(
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_2), INTER),
                generateInstances(Arrays.asList(OU_1, OU_2), Arrays.asList(account_1, account_3), INTER),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_3, account_4), DIFF),
                generateInstances(Arrays.asList(OU_1, OU_3), Arrays.asList(account_3, account_5), DIFF),
                generateInstances(OU_5)));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateDeleteInstances(OU_2, Arrays.asList(account_1, account_2, account_3), DIFF),
                generateDeleteInstances(OU_3, account_3, INTER),
                generateDeleteInstances(OU_4, Arrays.asList(account_3, account_4), DIFF)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, account_1, INTER),
                generateInstances(OU_2, account_1, INTER),
                generateInstances(OU_3, Arrays.asList(account_3, account_4, account_5), DIFF),
                generateInstances(OU_5, Collections.emptyList(), NONE)));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstances(OU_1, account_1, DIFF),
                generateInstances(OU_2, Arrays.asList(account_2, account_3), INTER),
                generateInstances(OU_3, Arrays.asList(account_4, account_5), INTER)));

        HashSet<StackInstances> stackInstancesSetToDelete = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToCreate = new HashSet<>();
        HashSet<StackInstances> stackInstancesSetToUpdate = new HashSet<>();
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = new HashMap<String, Set<StackInstances>>(){{put(region_1, currentGroup);}};
        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);
        new AltStackInstancesCalculator(region_1, previousGroup, currentGroup)
                .calculate(stackInstancesSetToDelete, stackInstancesSetToCreate, stackInstancesSetToUpdate, ouDeploymentParametersMap);

        assertThat(Comparator.equals(stackInstancesSetToDelete, desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToCreate, desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(stackInstancesSetToUpdate, desiredUpdateInstances)).isTrue();
    }

    private static HashMap<String, Set<Parameter>> findDeploymentParametersForOUs(final HashMap<String, Set<StackInstances>> currentStackInstancesByRegion) {
        HashMap<String, Set<Parameter>> ouDeploymentParameters = new HashMap<>();

        for (final String region : currentStackInstancesByRegion.keySet()) {
            for (final StackInstances stackInstances : currentStackInstancesByRegion.get(region)) {
                Set<Parameter> parameters = stackInstances.getParameterOverrides();

                stackInstances.getDeploymentTargets().getOrganizationalUnitIds().forEach(
                        ou -> ouDeploymentParameters.put(ou, parameters)
                );
            }
        }
        return ouDeploymentParameters;
    }
}
