# AWS::CloudFormation::StackSet Tag

Tag type enables you to specify a key-value pair that can be used to store information about an AWS CloudFormation StackSet.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#key" title="Key">Key</a>" : <i>String</i>,
    "<a href="#value" title="Value">Value</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#key" title="Key">Key</a>: <i>String</i>
<a href="#value" title="Value">Value</a>: <i>String</i>
</pre>

## Properties

#### Key

A string used to identify this tag. You can specify a maximum of 127 characters for a tag key.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>128</code>

_Pattern_: <code>^(?!aws:.*)[a-zA-Z0-9\s\:\_\.\/\=\+\-]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Value

A string containing the value for this tag. You can specify a maximum of 256 characters for a tag value.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
