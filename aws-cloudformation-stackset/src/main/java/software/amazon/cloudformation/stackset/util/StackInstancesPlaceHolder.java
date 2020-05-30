package software.amazon.cloudformation.stackset.util;

import lombok.Data;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.ArrayList;
import java.util.List;

@Data
public class StackInstancesPlaceHolder {

    private List<StackInstances> createStackInstances = new ArrayList<>();

    private List<StackInstances> deleteStackInstances = new ArrayList<>();

    private List<StackInstances> updateStackInstances = new ArrayList<>();
}
