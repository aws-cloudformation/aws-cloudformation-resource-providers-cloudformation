# AWS::CloudFormation::ResourceDefaultVersion

The default version of a resource that has been registered in the CloudFormation Registry.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::ResourceDefaultVersion",
    "Properties" : {
        "<a href="#arn" title="Arn">Arn</a>" : <i>String</i>,
        "<a href="#typename" title="TypeName">TypeName</a>" : <i>String</i>,
        "<a href="#versionid" title="VersionId">VersionId</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::ResourceDefaultVersion
Properties:
    <a href="#arn" title="Arn">Arn</a>: <i>String</i>
    <a href="#typename" title="TypeName">TypeName</a>: <i>String</i>
    <a href="#versionid" title="VersionId">VersionId</a>: <i>String</i>
</pre>

## Properties

#### Arn

The Amazon Resource Name (ARN) of the type version.

_Required_: No

_Type_: String

_Pattern_: <code>^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/resource/.+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TypeName

The name of the type being registered.

We recommend that type names adhere to the following pattern: company_or_organization::service::type.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VersionId

The ID of an existing version of the resource to set as the default.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9-]{1,128]$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.
