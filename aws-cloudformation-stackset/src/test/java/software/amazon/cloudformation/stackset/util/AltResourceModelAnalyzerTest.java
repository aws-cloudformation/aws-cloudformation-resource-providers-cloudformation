package software.amazon.cloudformation.stackset.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;
import static junit.framework.Assert.assertEquals;
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

        Set<StackInstances> desiredDeleteInstances = new HashSet<>();
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)
        ));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

    @Test
    public void test_Null_Current_Model() {
        ResourceModel previousModel = generateModel(new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), new HashSet<>(Arrays.asList(region_1, region_2))),
                generateInstancesWithRegions(
                        Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER,
                        new HashSet<>(Arrays.asList(region_1, region_3)))
        )));

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_2),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_1),
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_1, account_2), INTER, region_3)));
        Set<StackInstances> desiredCreateInstances = new HashSet<>();
        Set<StackInstances> desiredUpdateInstances = new HashSet<>();

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
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

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), region_3),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE, region_1),
                generateDeleteInstances(OU_2, Collections.emptyList(), NONE, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_3, OU_4), region_4)
        ));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_1),
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
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

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateDeleteInstances(OU_1, Collections.emptyList(), NONE, region_2),
                generateDeleteInstances(OU_2, Arrays.asList(account_2, account_3), DIFF, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER, region_3),
                generateInstancesWithRegions(OU_3, Arrays.asList(account_2, account_3), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
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

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_2, OU_3), Arrays.asList(account_2, account_3), INTER, region_3),
                generateDeleteInstances(OU_3, Arrays.asList(account_2, account_3), INTER, region_2)
        ));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(Arrays.asList(OU_1, OU_2), region_1),
                generateInstancesWithRegions(OU_1, Collections.emptyList(), NONE, region_2),
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), DIFF, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_2, Arrays.asList(account_2, account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
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

        Set<StackInstances> desiredDeleteInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_1, account_2), DIFF, region_1),
                generateDeleteInstances(OU_1, Arrays.asList(account_1, account_2, account_3), DIFF, region_2)

        ));
        Set<StackInstances> desiredCreateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Arrays.asList(account_2, account_3), INTER, region_3),
                generateInstancesWithRegions(OU_1, Collections.singletonList(account_2), INTER, region_2)
        ));
        Set<StackInstances> desiredUpdateInstances = new HashSet<>(Arrays.asList(
                generateInstancesWithRegions(OU_1, Collections.singletonList(account_3), INTER, region_2)
        ));

        StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        AltResourceModelAnalyzer.builder().currentModel(currentModel).previousModel(previousModel).build().analyze(placeHolder);

        assertEquals(new HashSet<>(placeHolder.getDeleteStackInstances()), desiredDeleteInstances);
        assertEquals(new HashSet<>(placeHolder.getCreateStackInstances()), desiredCreateInstances);
        assertEquals(new HashSet<>(placeHolder.getUpdateStackInstances()), desiredUpdateInstances);
    }

}
