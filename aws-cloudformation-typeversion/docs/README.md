# AWS::CloudFormation::TypeVersion

A generic resourceType that is used to register any type in CloudFormation Registry

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::CloudFormation::TypeVersion",
    "Properties" : {
        "<a href="#executionrolearn" title="ExecutionRoleArn">ExecutionRoleArn</a>" : <i>String</i>,
        "<a href="#loggingconfig" title="LoggingConfig">LoggingConfig</a>" : <i><a href="loggingconfig.md">LoggingConfig</a></i>,
        "<a href="#schemahandlerpackage" title="SchemaHandlerPackage">SchemaHandlerPackage</a>" : <i>String</i>,
        "<a href="#typename" title="TypeName">TypeName</a>" : <i>String</i>,
        "<a href="#type" title="Type">Type</a>" : <i>String</i>,
        "<a href="#typearn" title="TypeArn">TypeArn</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::CloudFormation::TypeVersion
Properties:
    <a href="#executionrolearn" title="ExecutionRoleArn">ExecutionRoleArn</a>: <i>String</i>
    <a href="#loggingconfig" title="LoggingConfig">LoggingConfig</a>: <i><a href="loggingconfig.md">LoggingConfig</a></i>
    <a href="#schemahandlerpackage" title="SchemaHandlerPackage">SchemaHandlerPackage</a>: <i>String</i>
    <a href="#typename" title="TypeName">TypeName</a>: <i>String</i>
    <a href="#type" title="Type">Type</a>: <i>String</i>
    <a href="#typearn" title="TypeArn">TypeArn</a>: <i>String</i>
</pre>

## Properties

#### ExecutionRoleArn

The Amazon Resource Name (ARN) of the IAM execution role to use to register the type. If your resource type calls AWS APIs in any of its handlers, you must create an IAM execution role that includes the necessary permissions to call those AWS APIs, and provision that execution role in your account. CloudFormation then assumes that execution role to provide your resource type with the appropriate credentials.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LoggingConfig

_Required_: No

_Type_: <a href="loggingconfig.md">LoggingConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SchemaHandlerPackage

A url to the S3 bucket containing the extension project package that contains the necessary files for the extension you want to register.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TypeName

The name of the type being registered.

We recommend that type names adhere to the following pattern: company_or_organization::service::type.

_Required_: Yes

_Type_: String

_Pattern_: <code>^[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}::[A-Za-z0-9]{2,64}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Type

The kind of extension.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>RESOURCE</code> | <code>MODULE</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TypeArn

The Amazon Resource Name (ARN) of the type without the versionID.

_Required_: No

_Type_: String

_Pattern_: <code>^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/.+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the TypeVersionArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### TypeVersionArn

The Amazon Resource Name (ARN) of the type, here the TypeVersion

#### Description

The description of the registered type.

#### IsDefaultVersion

Indicates if this type version is the current default version

#### Schema

The schema that defines the type.

For more information on type schemas, see Resource Provider Schema in the CloudFormation CLI User Guide.

#### ProvisioningType

The provisioning behavior of the type. AWS CloudFormation determines the provisioning type during registration, based on the types of handlers in the schema handler package submitted.

#### Visibility

The scope at which the type is visible and usable in CloudFormation operations.

Valid values include:

PRIVATE: The type is only visible and usable within the account in which it is registered. Currently, AWS CloudFormation marks any types you register as PRIVATE.

PUBLIC: The type is publically visible and usable within any Amazon account.

#### SourceUrl

The URL of the source code for the type.

#### DocumentationUrl

The URL of a page providing detailed documentation for this type.

#### LastUpdated

When the specified type version was registered.

#### TimeCreated

When the specified type version was registered.

#### VersionId

The ID of the version of the type represented by this type instance.

