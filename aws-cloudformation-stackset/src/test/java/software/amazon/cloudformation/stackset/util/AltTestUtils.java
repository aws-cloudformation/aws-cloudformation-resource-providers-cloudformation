package software.amazon.cloudformation.stackset.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.Parameter;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

public class AltTestUtils {

    public final static String NONE = "NONE";
    public final static String INTER = "INTERSECTION";
    public final static String DIFF = "DIFFERENCE";

    public final static String OU_1 = "ou-example-1";
    public final static String OU_2 = "ou-example-2";
    public final static String OU_3 = "ou-example-3";
    public final static String OU_4 = "ou-example-4";
    public final static String OU_5 = "ou-example-5";

    public final static String account_1 = "11111";
    public final static String account_2 = "22222";
    public final static String account_3 = "33333";
    public final static String account_4 = "44444";
    public final static String account_5 = "55555";

    public final static String region_1 = "eu-east-1";
    public final static String region_2 = "us-west-1";
    public final static String region_3 = "us-east-2";
    public final static String region_4 = "us-east-1";

    public final static Set<String> singleton_region_set = new HashSet<>(Collections.singletonList(region_1));

    public final static Set<Parameter> parameters_1 = new HashSet<>(Collections.singletonList(
            Parameter.builder().parameterKey("K1").parameterValue("V1").build()));

    final static Set<Parameter> parameters_2 = new HashSet<>(Collections.singletonList(
            Parameter.builder().parameterKey("K2").parameterValue("V2").build()));

    /*
     * The following generateInstances methods all take (OUs, accounts, filter) as partial or complete inputs
     * */
    public static StackInstances generateInstances(List<String> ous, List<String> accounts, String filter) {
        return generateInstances(ous, accounts, filter, singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(List<String> ous, String account, String filter) {
        return generateInstances(ous, Collections.singletonList(account), filter, singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(String ou, List<String> accounts, String filter) {
        return generateInstances(Collections.singletonList(ou), accounts, filter, singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(String ou, String account, String filter) {
        return generateInstances(Collections.singletonList(ou), Collections.singletonList(account), filter, singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(List<String> ous, List<String> accounts, String filter, Set<Parameter> parameters) {
        return generateInstances(ous, accounts, filter, singleton_region_set, parameters);
    }

    public static StackInstances generateInstances(String ou, List<String> accounts, String filter, Set<Parameter> parameters) {
        return generateInstances(Collections.singletonList(ou), accounts, filter, singleton_region_set, parameters);
    }

    public static StackInstances generateInstancesWithRegions(List<String> ous, List<String> accounts, String filter, Set<String> regions) {
        return generateInstances(ous, accounts, filter, regions, parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(String ou, List<String> accounts, String filter, Set<String> regions) {
        return generateInstances(Collections.singletonList(ou), accounts, filter, regions, parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(List<String> ous, List<String> accounts, String filter, String region) {
        return generateInstances(ous, accounts, filter, new HashSet<>(Collections.singletonList(region)), parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(String ou, List<String> accounts, String filter, String region) {
        return generateInstances(Collections.singletonList(ou), accounts, filter, new HashSet<>(Collections.singletonList(region)), parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(String ou, String account, String filter, String region) {
        return generateInstances(Collections.singletonList(ou), Collections.singletonList(account), filter, new HashSet<>(Collections.singletonList(region)), parameters_1);
    }

    public static StackInstances generateInstances(List<String> ous, List<String> accounts, String filter, Set<String> regions, Set<Parameter> parameters) {
        DeploymentTargets targets = DeploymentTargets.builder()
                .organizationalUnitIds(new HashSet<>(ous))
                .accounts(new HashSet<>(accounts))
                .accountFilterType(filter).build();
        return StackInstances.builder()
                .deploymentTargets(targets)
                .regions(regions)
                .parameterOverrides(parameters).build();
    }

    /*
    * The following generateInstance methods only take OUs as input
    * No accounts or ALT filter set in the deploymentTarget
    * */

    public static StackInstances generateInstances(String ou) {
        return generateInstances(Collections.singletonList(ou), singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(List<String> ous) {
        return generateInstances(ous, singleton_region_set, parameters_1);
    }

    public static StackInstances generateInstances(String ou, Set<Parameter> parameters) {
        return generateInstances(Collections.singletonList(ou), singleton_region_set, parameters);
    }

    public static StackInstances generateInstances(List<String> ous, Set<Parameter> parameters) {
        return generateInstances(ous, singleton_region_set, parameters);
    }

    public static StackInstances generateInstancesWithRegions(List<String> ous, Set<String> regions) {
        return generateInstances(ous, regions, parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(String ou, Set<String> regions) {
        return generateInstances(Collections.singletonList(ou), regions, parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(List<String> ous, String region) {
        return generateInstances(ous, new HashSet<>(Collections.singletonList(region)), parameters_1);
    }

    public static StackInstances generateInstancesWithRegions(String ou, String region) {
        return generateInstances(Collections.singletonList(ou), new HashSet<>(Collections.singletonList(region)), parameters_1);
    }

    public static StackInstances generateInstances(List<String> ous, Set<String> regions, Set<Parameter> parameters) {
        return StackInstances.builder()
                .deploymentTargets(DeploymentTargets.builder().organizationalUnitIds(new HashSet<>(ous)).build())
                .regions(regions)
                .parameterOverrides(parameters)
                .build();
    }

    /*
    * StackInstances to delete don't have parameters
    * */

    public static StackInstances generateDeleteInstances(List<String> ous, List<String> accounts, String filter) {
        return generateDeleteInstances(ous, accounts, filter, singleton_region_set);
    }

    public static StackInstances generateDeleteInstances(String ou, List<String> accounts, String filter) {
        return generateDeleteInstances(Collections.singletonList(ou), accounts, filter, singleton_region_set);
    }

    public static StackInstances generateDeleteInstances(String ou, String account, String filter) {
        return generateDeleteInstances(Collections.singletonList(ou), Collections.singletonList(account), filter, singleton_region_set);
    }

    public static StackInstances generateDeleteInstances(String ou, List<String> accounts, String filter, String region) {
        return generateDeleteInstances(Collections.singletonList(ou), accounts, filter, new HashSet<>(Collections.singletonList(region)));
    }

    public static StackInstances generateDeleteInstances(List<String> ous, List<String> accounts, String filter, Set<String> regions) {
        DeploymentTargets targets = DeploymentTargets.builder()
                .organizationalUnitIds(new HashSet<>(ous))
                .accounts(new HashSet<>(accounts))
                .accountFilterType(filter).build();

        return StackInstances.builder()
                .deploymentTargets(targets)
                .regions(regions)
                .build();
    }

    public static ResourceModel generateModel (Set<StackInstances> instancesGroup) {
        return ResourceModel.builder().stackInstancesGroup(instancesGroup).build();
    }
}
