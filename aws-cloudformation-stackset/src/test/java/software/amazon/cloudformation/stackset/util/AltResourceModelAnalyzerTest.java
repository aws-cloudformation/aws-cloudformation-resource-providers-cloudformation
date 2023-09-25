package software.amazon.cloudformation.stackset.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.ManagedExecution;
import software.amazon.cloudformation.stackset.OperationPreferences;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.DIFF;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.INTER;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.NONE;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_4;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateDeleteInstances;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstances;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstancesWithRegions;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateModel;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.parameters_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.parameters_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_3;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_4;

public class AltResourceModelAnalyzerTest {

    @Test
    public void test_Invalid_Alt_Model() {
        String invalidFilterType = "UNION";

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();

        ResourceModel modelEmptyOU = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(new ArrayList<>(), OU_1, INTER))));

        ResourceModel modelEmptyAccounts = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(OU_1, new ArrayList<>(), INTER))));

        ResourceModel modelInvalidFilter = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(OU_1, account_1, invalidFilterType))));

        ResourceModel modelNoneFilter = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(OU_1, account_1, NONE))));

        ResourceModel modelNullFilter = generateModel(new HashSet<>(Arrays.asList(
                StackInstances.builder().deploymentTargets(
                                DeploymentTargets.builder()
                                        .organizationalUnitIds(new HashSet<>(Arrays.asList(OU_1)))
                                        .accounts(new HashSet<>(Arrays.asList(account_1)))
                                        .build())
                        .build()
        )));

        ResourceModel modelPartiallyInvalid = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(OU_1, account_1, INTER),
                generateInstances(OU_1, account_1, NONE))));

        Exception ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelEmptyOU).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains("OrganizationalUnitIds should be specified in SERVICE_MANAGED mode");


        ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelEmptyAccounts).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains("Accounts should be specified when Account-level Targeting is enabled");

        ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelInvalidFilter).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains(String.format("%s is not a valid AccountFilterType", invalidFilterType));

        ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelNoneFilter).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains("AccountFilterType should be specified when both OrganizationalUnitIds and Accounts are provided");

        ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelNullFilter).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains("AccountFilterType should be specified when both OrganizationalUnitIds and Accounts are provided");

        assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(modelPartiallyInvalid).build().analyze(placeHolder)
        );
    }

    @Test
    public void test_Null_Previous_Model() {
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), new HashSet<>(Arrays.asList(region_1, region_2))),
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Null_Current_Model() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), new HashSet<>(Arrays.asList(region_1, region_2))),
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_ALT_Create_With_OperationPreferences_And_Region_Order() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        List<String> regionOrder = Arrays.asList(region_3, region_2, region_1);
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);
        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Create_With_OperationPreferences_And_Region_Order_And_ME() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        List<String> regionOrder = Arrays.asList(region_3, region_2, region_1);
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));
        currentModel.setManagedExecution(new ManagedExecution(true));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);
        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Create_With_Less_Number_Of_Regions_In_Region_Order() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        List<String> regionOrder = Arrays.asList(region_3);
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);
        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }
    @Test
    public void test_ALT_Create_With_InCorrect_Region_Order() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));
        List<String> regionOrder = Arrays.asList(region_3);
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);
        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Create_With_Less_Number_Of_Regions() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));
        List<String> regionOrder = Arrays.asList(region_3, region_2, region_1);
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);
        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Delete_With_OperationPreferences_And_Region_Order() {
        ResourceModel previousModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        List<String> regionOrder = Arrays.asList(region_3, region_2, region_1);
        previousModel.setOperationPreferences(new OperationPreferences(null, null, null, null, regionOrder, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).build().analyze(placeHolder);

        Iterator<StackInstances> iterator = desiredDeleteInstances.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getDeleteStackInstances().get(index));
            index++;
        }
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Create_With_Null_Region_Order_And_Null_OperationPreferences() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);

        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Create_With_Null_Region_Order_And_Not_Null_OperationPreferences() {
        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        currentModel.setOperationPreferences(new OperationPreferences(null, null, null, null, null, null));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);

        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Delete_With_Null_Region_Order_And_Not_Null_OperationPreferences() {
        ResourceModel previousModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));
        previousModel.setOperationPreferences(new OperationPreferences(null, null, null, null, null, null));

        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Delete_With_Null_Region_Order_And_Null_OperationPreferences() {
        ResourceModel previousModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>();
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Update_With_Null_Region_Order() {
        ResourceModel previousModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_4)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Collections.singletonList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Collections.singletonList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_4)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Collections.singletonList(OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Collections.singletonList(OU_2), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Collections.singletonList(OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Collections.singletonList(OU_2), Arrays.asList(account_1, account_2), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).currentModel(currentModel).build().analyze(placeHolder);

        assertEquals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_ALT_Update_With_Region_Order() {
        ResourceModel previousModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_3)))
        )));

        ResourceModel currentModel = generateModel(new HashSet<>(Collections.singletonList(
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_2, region_4)))
        )));
        List<String> regionOrder = Arrays.asList(region_4, region_3, region_2, region_1);
        OperationPreferences operationPreferences = new OperationPreferences(null, null, null, null, regionOrder, null);
        currentModel.setOperationPreferences(operationPreferences);
        previousModel.setOperationPreferences(operationPreferences);

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Collections.singletonList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Collections.singletonList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_4)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Collections.singletonList(OU_3), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Collections.singletonList(OU_2), Arrays.asList(account_1, account_2), INTER, region_2),
                generateInstancesWithRegions(Collections.singletonList(OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Collections.singletonList(OU_2), Arrays.asList(account_1, account_2), INTER, region_1)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).currentModel(currentModel).build().analyze(placeHolder);

        Iterator<StackInstances> iterator = desiredCreateInstances.iterator();

        int index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getCreateStackInstances().get(index));
            index++;
        }
        iterator = desiredDeleteInstances.iterator();
        index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getDeleteStackInstances().get(index));
            index++;
        }

        iterator = desiredUpdateInstances.iterator();
        index = 0;
        while (iterator.hasNext()) {
            StackInstances stackInstances = iterator.next();
            assertEquals(stackInstances, placeHolder.getUpdateStackInstances().get(index));
            index++;
        }
    }

    @Test
    public void test_No_Alt_In_Neither_Models() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), new HashSet<>(Arrays.asList(region_1, region_2))),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), region_3)
        )));
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, new HashSet<>(Arrays.asList(region_1, region_2))),
                generateInstancesWithRegions(Arrays.asList(OU_3, OU_4), region_4)
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), region_3),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE, region_1),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_3, OU_4), region_4)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_1),
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_No_Alt_In_Previous_Model() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2),
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER,
                        new HashSet<>(Arrays.asList(region_2, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE, region_2),
                generateDeleteInstances(OU_2, Arrays.asList(account_2, account_3), DIFF, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER, region_3),
                generateInstancesWithRegions(OU_3, Arrays.asList(account_2, account_3), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_No_Alt_In_Current_Model() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER,
                        new HashSet<>(Arrays.asList(region_2, region_3)))
        )));
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2),
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER, region_3),
                generateDeleteInstances(OU_3, Arrays.asList(account_2, account_3), INTER, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_2),
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), DIFF, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_Alt_In_Both_Models() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF,
                        new HashSet<>(Arrays.asList(region_1, region_2)))
        )));
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_2, account_3), INTER,
                        new HashSet<>(Arrays.asList(region_2, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF, region_1),
                generateDeleteInstances(OU_1, Arrays.asList(account_1, account_2, account_3), DIFF, region_2)

        ));
        Set<StackInstances> desiredCreateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_2, account_3), INTER, region_3),
                generateInstancesWithRegions(OU_1, Collections.singletonList(account_2), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new LinkedHashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Collections.singletonList(account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances)).isTrue();
        assertThat(Comparator.equals(new LinkedHashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances)).isTrue();
    }

    @Test
    public void test_No_Alt_Filter_Multiple_Parameters_In_One_Group() {
        ResourceModel currentModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstances(OU_1, parameters_1),
                generateInstances(Arrays.asList(OU_1, OU_2), parameters_2)))
        );
        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();

        Exception ex = assertThrows(
                CfnInvalidRequestException.class,
                () -> AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder)
        );
        assertThat(ex.getMessage()).contains("An OrganizationalUnitIds cannot be associated with more than one Parameters set");
    }
}
