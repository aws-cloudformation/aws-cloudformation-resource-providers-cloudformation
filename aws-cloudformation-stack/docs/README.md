# AWS::CloudFormation::Stack

The AWS::CloudFormation::Stack resource nests a stack as a resource in a top-level template.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::Stack",
    "Properties" : {
        "<a href="#capabilities" title="Capabilities">Capabilities</a>" : <i>[ String, ... ]</i>,
        "<a href="#rolearn" title="RoleARN">RoleARN</a>" : <i>String</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#disablerollback" title="DisableRollback">DisableRollback</a>" : <i>Boolean</i>,
        "<a href="#enableterminationprotection" title="EnableTerminationProtection">EnableTerminationProtection</a>" : <i>Boolean</i>,
        "<a href="#notificationarns" title="NotificationARNs">NotificationARNs</a>" : <i>[ String, ... ]</i>,
        "<a href="#parameters" title="Parameters">Parameters</a>" : <i><a href="parameters.md">Parameters</a></i>,
        "<a href="#stackname" title="StackName">StackName</a>" : <i>String</i>,
        "<a href="#stackpolicybody" title="StackPolicyBody">StackPolicyBody</a>" : <i>Map</i>,
        "<a href="#stackpolicyurl" title="StackPolicyURL">StackPolicyURL</a>" : <i>String</i>,
        "<a href="#stackstatusreason" title="StackStatusReason">StackStatusReason</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#templatebody" title="TemplateBody">TemplateBody</a>" : <i>String</i>,
        "<a href="#templateurl" title="TemplateURL">TemplateURL</a>" : <i>String</i>,
        "<a href="#timeoutinminutes" title="TimeoutInMinutes">TimeoutInMinutes</a>" : <i>Integer</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::Stack
Properties:
    <a href="#capabilities" title="Capabilities">Capabilities</a>: <i>
      - String</i>
    <a href="#rolearn" title="RoleARN">RoleARN</a>: <i>String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#disablerollback" title="DisableRollback">DisableRollback</a>: <i>Boolean</i>
    <a href="#enableterminationprotection" title="EnableTerminationProtection">EnableTerminationProtection</a>: <i>Boolean</i>
    <a href="#notificationarns" title="NotificationARNs">NotificationARNs</a>: <i>
      - String</i>
    <a href="#parameters" title="Parameters">Parameters</a>: <i><a href="parameters.md">Parameters</a></i>
    <a href="#stackname" title="StackName">StackName</a>: <i>String</i>
    <a href="#stackpolicybody" title="StackPolicyBody">StackPolicyBody</a>: <i>Map</i>
    <a href="#stackpolicyurl" title="StackPolicyURL">StackPolicyURL</a>: <i>String</i>
    <a href="#stackstatusreason" title="StackStatusReason">StackStatusReason</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#templatebody" title="TemplateBody">TemplateBody</a>: <i>String</i>
    <a href="#templateurl" title="TemplateURL">TemplateURL</a>: <i>String</i>
    <a href="#timeoutinminutes" title="TimeoutInMinutes">TimeoutInMinutes</a>: <i>Integer</i>
</pre>

## Properties

#### Capabilities

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RoleARN

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Description

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DisableRollback

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EnableTerminationProtection

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NotificationARNs

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Parameters

_Required_: No

_Type_: <a href="parameters.md">Parameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StackName

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### StackPolicyBody

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StackPolicyURL

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StackStatusReason

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TemplateBody

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TemplateURL

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TimeoutInMinutes

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the StackId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### StackId

Returns the <code>StackId</code> value.

#### StackStatus

Returns the <code>StackStatus</code> value.

#### CreationTime

Returns the <code>CreationTime</code> value.

#### RootId

Returns the <code>RootId</code> value.

#### ParentId

Returns the <code>ParentId</code> value.

#### ChangeSetId

Returns the <code>ChangeSetId</code> value.

#### Outputs

Returns the <code>Outputs</code> value.

#### LastUpdateTime

Returns the <code>LastUpdateTime</code> value.
