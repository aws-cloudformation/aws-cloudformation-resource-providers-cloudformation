# AWS::CloudFormation::HookVersion

Publishes new or first hook version to AWS CloudFormation Registry.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::HookVersion",
    "Properties" : {
        "<a href="#executionrolearn" title="ExecutionRoleArn">ExecutionRoleArn</a>" : <i>String</i>,
        "<a href="#loggingconfig" title="LoggingConfig">LoggingConfig</a>" : <i><a href="loggingconfig.md">LoggingConfig</a></i>,
        "<a href="#schemahandlerpackage" title="SchemaHandlerPackage">SchemaHandlerPackage</a>" : <i>String</i>,
        "<a href="#typename" title="TypeName">TypeName</a>" : <i>String</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::HookVersion
Properties:
    <a href="#executionrolearn" title="ExecutionRoleArn">ExecutionRoleArn</a>: <i>String</i>
    <a href="#loggingconfig" title="LoggingConfig">LoggingConfig</a>: <i><a href="loggingconfig.md">LoggingConfig</a></i>
    <a href="#schemahandlerpackage" title="SchemaHandlerPackage">SchemaHandlerPackage</a>: <i>String</i>
    <a href="#typename" title="TypeName">TypeName</a>: <i>String</i>
</pre>

## Properties

#### ExecutionRoleArn

The Amazon Resource Name (ARN) of the IAM execution role to use to register the type. If your resource type calls AWS APIs in any of its handlers, you must create an IAM execution role that includes the necessary permissions to call those AWS APIs, and provision that execution role in your account. CloudFormation then assumes that execution role to provide your resource type with the appropriate credentials.

_Required_: No

_Type_: String

_Maximum_: <code>256</code>

_Pattern_: <code>arn:.+:iam::[0-9]{12}:role/.+</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### LoggingConfig

_Required_: No

_Type_: <a href="loggingconfig.md">LoggingConfig</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SchemaHandlerPackage

A url to the S3 bucket containing the schema handler package that contains the schema, event handlers, and associated files for the type you want to register.

For information on generating a schema handler package for the type you want to register, see submit in the CloudFormation CLI User Guide.

_Required_: Yes

_Type_: String

_Maximum_: <code>4096</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### TypeName

The name of the type being registered.

We recommend that type names adhere to the following pattern: company_or_organization::service::type.

_Required_: Yes

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The Amazon Resource Name (ARN) of the type, here the HookVersion. This is used to uniquely identify a HookVersion resource

#### IsDefaultVersion

Indicates if this type version is the current default version

#### ProvisioningType

Returns the <code>ProvisioningType</code> value.

#### Visibility

The scope at which the type is visible and usable in CloudFormation operations.

Valid values include:

PRIVATE: The type is only visible and usable within the account in which it is registered. Currently, AWS CloudFormation marks any types you register as PRIVATE.

PUBLIC: The type is publically visible and usable within any Amazon account.

#### VersionId

The ID of the version of the type represented by this hook instance.

#### TypeArn

The Amazon Resource Name (ARN) of the type without the versionID.
