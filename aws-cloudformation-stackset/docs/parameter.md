# AWS::CloudFormation::StackSet Parameter

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#parameterkey" title="ParameterKey">ParameterKey</a>" : <i>String</i>,
    "<a href="#parametervalue" title="ParameterValue">ParameterValue</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#parameterkey" title="ParameterKey">ParameterKey</a>: <i>String</i>
<a href="#parametervalue" title="ParameterValue">ParameterValue</a>: <i>String</i>
</pre>

## Properties

#### ParameterKey

The key associated with the parameter. If you don't specify a key and value for a particular parameter, AWS CloudFormation uses the default value that is specified in your template.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ParameterValue

The input value associated with the parameter.

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

