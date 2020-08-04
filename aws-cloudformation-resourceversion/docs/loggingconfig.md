# AWS::CloudFormation::ResourceVersion LoggingConfig

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#loggroupname" title="LogGroupName">LogGroupName</a>" : <i>String</i>,
    "<a href="#logrolearn" title="LogRoleArn">LogRoleArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#loggroupname" title="LogGroupName">LogGroupName</a>: <i>String</i>
<a href="#logrolearn" title="LogRoleArn">LogRoleArn</a>: <i>String</i>
</pre>

## Properties

#### LogGroupName

The Amazon CloudWatch log group to which CloudFormation sends error logging information when invoking the type's handlers.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>512</code>

_Pattern_: <code>^[\.\-_/#A-Za-z0-9]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LogRoleArn

The ARN of the role that CloudFormation should assume when sending log entries to CloudWatch logs.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
