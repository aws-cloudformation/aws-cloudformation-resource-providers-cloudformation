# AWS::CloudFormation::StackV2

Resource Type definition for AWS::CloudFormation::Stack

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::StackV2",
    "Properties" : {
        "<a href="#stackname" title="StackName">StackName</a>" : <i>String</i>,
        "<a href="#notificationarns" title="NotificationARNs">NotificationARNs</a>" : <i>[ String, ... ]</i>,
        "<a href="#parameters" title="Parameters">Parameters</a>" : <i><a href="parameters.md">Parameters</a></i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#templateurl" title="TemplateURL">TemplateURL</a>" : <i>String</i>,
        "<a href="#timeoutinminutes" title="TimeoutInMinutes">TimeoutInMinutes</a>" : <i>Integer</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::StackV2
Properties:
    <a href="#stackname" title="StackName">StackName</a>: <i>String</i>
    <a href="#notificationarns" title="NotificationARNs">NotificationARNs</a>: <i>
      - String</i>
    <a href="#parameters" title="Parameters">Parameters</a>: <i><a href="parameters.md">Parameters</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#templateurl" title="TemplateURL">TemplateURL</a>: <i>String</i>
    <a href="#timeoutinminutes" title="TimeoutInMinutes">TimeoutInMinutes</a>: <i>Integer</i>
</pre>

## Properties

#### StackName

_Required_: No

_Type_: String

_Maximum_: <code>128</code>

_Pattern_: <code>^[A-Za-z][A-Za-z0-9-]*$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NotificationARNs

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Parameters

_Required_: No

_Type_: <a href="parameters.md">Parameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TemplateURL

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TimeoutInMinutes

_Required_: No

_Type_: Integer

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

Returns the <code>Arn</code> value.
