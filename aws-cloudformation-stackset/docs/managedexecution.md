# AWS::CloudFormation::StackSet ManagedExecution

Describes whether StackSets performs non-conflicting operations concurrently and queues conflicting operations.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#active" title="Active">Active</a>" : <i>Boolean</i>
}
</pre>

### YAML

<pre>
<a href="#active" title="Active">Active</a>: <i>Boolean</i>
</pre>

## Properties

#### Active

When true, StackSets performs non-conflicting operations concurrently and queues conflicting operations. After conflicting operations finish, StackSets starts queued operations in request order.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
