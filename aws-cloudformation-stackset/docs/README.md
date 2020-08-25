# PGE::CloudFormation::StackSet

StackSet as a resource provides one-click experience for provisioning a StackSet and StackInstances

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "PGE::CloudFormation::StackSet",
    "Properties" : {
        "<a href="#stacksetname" title="StackSetName">StackSetName</a>" : <i>String</i>,
        "<a href="#administrationrolearn" title="AdministrationRoleARN">AdministrationRoleARN</a>" : <i>String</i>,
        "<a href="#autodeployment" title="AutoDeployment">AutoDeployment</a>" : <i><a href="autodeployment.md">AutoDeployment</a></i>,
        "<a href="#capabilities" title="Capabilities">Capabilities</a>" : <i>[ String, ... ]</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#executionrolename" title="ExecutionRoleName">ExecutionRoleName</a>" : <i>String</i>,
        "<a href="#operationpreferences" title="OperationPreferences">OperationPreferences</a>" : <i><a href="operationpreferences.md">OperationPreferences</a></i>,
        "<a href="#stackinstancesgroup" title="StackInstancesGroup">StackInstancesGroup</a>" : <i>[ <a href="stackinstances.md">StackInstances</a>, ... ]</i>,
        "<a href="#parameters" title="Parameters">Parameters</a>" : <i>[ <a href="parameter.md">Parameter</a>, ... ]</i>,
        "<a href="#permissionmodel" title="PermissionModel">PermissionModel</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#templatebody" title="TemplateBody">TemplateBody</a>" : <i>String</i>,
        "<a href="#templateurl" title="TemplateURL">TemplateURL</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: PGE::CloudFormation::StackSet
Properties:
    <a href="#stacksetname" title="StackSetName">StackSetName</a>: <i>String</i>
    <a href="#administrationrolearn" title="AdministrationRoleARN">AdministrationRoleARN</a>: <i>String</i>
    <a href="#autodeployment" title="AutoDeployment">AutoDeployment</a>: <i><a href="autodeployment.md">AutoDeployment</a></i>
    <a href="#capabilities" title="Capabilities">Capabilities</a>: <i>
      - String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#executionrolename" title="ExecutionRoleName">ExecutionRoleName</a>: <i>String</i>
    <a href="#operationpreferences" title="OperationPreferences">OperationPreferences</a>: <i><a href="operationpreferences.md">OperationPreferences</a></i>
    <a href="#stackinstancesgroup" title="StackInstancesGroup">StackInstancesGroup</a>: <i>
      - <a href="stackinstances.md">StackInstances</a></i>
    <a href="#parameters" title="Parameters">Parameters</a>: <i>
      - <a href="parameter.md">Parameter</a></i>
    <a href="#permissionmodel" title="PermissionModel">PermissionModel</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#templatebody" title="TemplateBody">TemplateBody</a>: <i>String</i>
    <a href="#templateurl" title="TemplateURL">TemplateURL</a>: <i>String</i>
</pre>

## Properties

#### StackSetName

The name to associate with the stack set. The name must be unique in the Region where you create your stack set.

_Required_: No

_Type_: String

_Maximum_: <code>128</code>

_Pattern_: <code>^[a-zA-Z][a-zA-Z0-9\-]{0,127}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### AdministrationRoleARN

The Amazon Resource Number (ARN) of the IAM role to use to create this stack set. Specify an IAM role only if you are using customized administrator roles to control which users or groups can manage specific stack sets within the same administrator account.

_Required_: No

_Type_: String

_Minimum_: <code>20</code>

_Maximum_: <code>2048</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AutoDeployment

_Required_: No

_Type_: <a href="autodeployment.md">AutoDeployment</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Capabilities

In some cases, you must explicitly acknowledge that your stack set template contains certain capabilities in order for AWS CloudFormation to create the stack set and related stack instances.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Description

A description of the stack set. You can use the description to identify the stack set's purpose or other important information.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ExecutionRoleName

The name of the IAM execution role to use to create the stack set. If you do not specify an execution role, AWS CloudFormation uses the AWSCloudFormationStackSetExecutionRole role for the stack set operation.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>64</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OperationPreferences

The user-specified preferences for how AWS CloudFormation performs a stack set operation.

_Required_: No

_Type_: <a href="operationpreferences.md">OperationPreferences</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StackInstancesGroup

_Required_: No

_Type_: List of <a href="stackinstances.md">StackInstances</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Parameters

The input parameters for the stack set template.

_Required_: No

_Type_: List of <a href="parameter.md">Parameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PermissionModel

Describes how the IAM roles required for stack set operations are created. By default, SELF-MANAGED is specified.

_Required_: No

_Type_: String

_Allowed Values_: <code>SERVICE_MANAGED</code> | <code>SELF_MANAGED</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

The key-value pairs to associate with this stack set and the stacks created from it. AWS CloudFormation also propagates these tags to supported resources that are created in the stacks. A maximum number of 50 tags can be specified.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TemplateBody

The structure that contains the template body, with a minimum length of 1 byte and a maximum length of 51,200 bytes.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>51200</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TemplateURL

Location of file containing the template body. The URL must point to a template (max size: 460,800 bytes) that is located in an Amazon S3 bucket.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the StackSetId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### StackSetId

The ID of the stack set that you're creating.

