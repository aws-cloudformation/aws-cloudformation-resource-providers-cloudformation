# AWS::CloudFormation::ModuleDefaultVersion

A module that has been registered in the CloudFormation registry as the default version

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::ModuleDefaultVersion",
    "Properties" : {
        "<a href="#arn" title="Arn">Arn</a>" : <i>String</i>,
        "<a href="#modulename" title="ModuleName">ModuleName</a>" : <i>String</i>,
        "<a href="#versionid" title="VersionId">VersionId</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::ModuleDefaultVersion
Properties:
    <a href="#arn" title="Arn">Arn</a>: <i>String</i>
    <a href="#modulename" title="ModuleName">ModuleName</a>: <i>String</i>
    <a href="#versionid" title="VersionId">VersionId</a>: <i>String</i>
</pre>

## Properties

#### Arn

The Amazon Resource Name (ARN) of the module version to set as the default version.

_Required_: No

_Type_: String

_Pattern_: <code>^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/module/.+/[0-9]{8}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ModuleName

The name of a module existing in the registry.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::MODULE</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### VersionId

The ID of an existing version of the named module to set as the default.

_Required_: No

_Type_: String

_Pattern_: <code>^[0-9]{8}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.
