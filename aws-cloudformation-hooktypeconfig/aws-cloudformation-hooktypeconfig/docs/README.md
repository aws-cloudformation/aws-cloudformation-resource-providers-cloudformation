# AWS::CloudFormation::HookTypeConfig

Specifies the configuration data for a registered hook in CloudFormation Registry.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::HookTypeConfig",
    "Properties" : {
        "<a href="#typearn" title="TypeArn">TypeArn</a>" : <i>String</i>,
        "<a href="#typename" title="TypeName">TypeName</a>" : <i>String</i>,
        "<a href="#configuration" title="Configuration">Configuration</a>" : <i>String</i>,
        "<a href="#configurationalias" title="ConfigurationAlias">ConfigurationAlias</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::HookTypeConfig
Properties:
    <a href="#typearn" title="TypeArn">TypeArn</a>: <i>String</i>
    <a href="#typename" title="TypeName">TypeName</a>: <i>String</i>
    <a href="#configuration" title="Configuration">Configuration</a>: <i>String</i>
    <a href="#configurationalias" title="ConfigurationAlias">ConfigurationAlias</a>: <i>String</i>
</pre>

## Properties

#### TypeArn

The Amazon Resource Name (ARN) of the type without version number.

_Required_: No

_Type_: String

_Pattern_: <code>^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/hook/.+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TypeName

The name of the type being registered.

We recommend that type names adhere to the following pattern: company_or_organization::service::type.

_Required_: No

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Configuration

The configuration data for the extension, in this account and region.

_Required_: No

_Type_: String

_Pattern_: <code>[\s\S]+</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConfigurationAlias

An alias by which to refer to this extension configuration data.

_Required_: No

_Type_: String

_Allowed Values_: <code>default</code>

_Pattern_: <code>^[a-zA-Z0-9]{1,256}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ConfigurationArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ConfigurationArn

The Amazon Resource Name (ARN) for the configuration data, in this account and region.
