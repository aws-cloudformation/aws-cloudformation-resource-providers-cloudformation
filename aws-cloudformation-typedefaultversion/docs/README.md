# AWS::CloudFormation::TypeDefaultVersion

A generic resourceType that is used to set default version for any type in CloudFormation Registry

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::TypeDefaultVersion",
    "Properties" : {
        "<a href="#typeversionarn" title="TypeVersionArn">TypeVersionArn</a>" : <i>String</i>,
        "<a href="#typename" title="TypeName">TypeName</a>" : <i>String</i>,
<<<<<<< HEAD
        "<a href="#versionid" title="VersionId">VersionId</a>" : <i>String</i>,
        "<a href="#type" title="Type">Type</a>" : <i>String</i>
=======
        "<a href="#versionid" title="VersionId">VersionId</a>" : <i>String</i>
>>>>>>> c59a4c88cc6bf2fdb668b328db4c824a2ff821c6
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::TypeDefaultVersion
Properties:
    <a href="#typeversionarn" title="TypeVersionArn">TypeVersionArn</a>: <i>String</i>
    <a href="#typename" title="TypeName">TypeName</a>: <i>String</i>
    <a href="#versionid" title="VersionId">VersionId</a>: <i>String</i>
<<<<<<< HEAD
    <a href="#type" title="Type">Type</a>: <i>String</i>
=======
>>>>>>> c59a4c88cc6bf2fdb668b328db4c824a2ff821c6
</pre>

## Properties

#### TypeVersionArn

The Amazon Resource Name (ARN) of the TypeVersion.

_Required_: No

_Type_: String

_Pattern_: <code>^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/.+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TypeName

The name of the type being registered.

We recommend that type names adhere to the following pattern: company_or_organization::service::type.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VersionId

The ID of an existing version of the type to set as the default.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9-]{1,128}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

<<<<<<< HEAD
#### Type

The kind of extension.

_Required_: No

_Type_: String

_Allowed Values_: <code>RESOURCE</code> | <code>MODULE</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

=======
>>>>>>> c59a4c88cc6bf2fdb668b328db4c824a2ff821c6
## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the TypeArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### TypeArn

<<<<<<< HEAD
The Amazon Resource Name (ARN) of the type without the versionID. This is used to uniquely identify a TypeDefaultVersion resource
=======
The Amazon Resource Name (ARN) of the type without the versionID. This is used to uniquely identify a TypeDefaultVersion
>>>>>>> c59a4c88cc6bf2fdb668b328db4c824a2ff821c6
