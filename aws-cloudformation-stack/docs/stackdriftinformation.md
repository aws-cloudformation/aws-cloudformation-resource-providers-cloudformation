# AWS::CloudFormation::Stack StackDriftInformation

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#lastchecktimestamp" title="LastCheckTimestamp">LastCheckTimestamp</a>" : <i>String</i>,
    "<a href="#stackdriftstatus" title="StackDriftStatus">StackDriftStatus</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#lastchecktimestamp" title="LastCheckTimestamp">LastCheckTimestamp</a>: <i>String</i>
<a href="#stackdriftstatus" title="StackDriftStatus">StackDriftStatus</a>: <i>String</i>
</pre>

## Properties

#### LastCheckTimestamp

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### StackDriftStatus

_Required_: Yes

_Type_: String

_Allowed Values_: <code>DRIFTED</code> | <code>IN_SYNC</code> | <code>UNKNOWN</code> | <code>NOT_CHECKED</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
