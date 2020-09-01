# PGE::CloudFormation::StackSet OperationPreferences

The user-specified preferences for how AWS CloudFormation performs a stack set operation.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#failuretolerancecount" title="FailureToleranceCount">FailureToleranceCount</a>" : <i>Integer</i>,
    "<a href="#failuretolerancepercentage" title="FailureTolerancePercentage">FailureTolerancePercentage</a>" : <i>Integer</i>,
    "<a href="#maxconcurrentcount" title="MaxConcurrentCount">MaxConcurrentCount</a>" : <i>Integer</i>,
    "<a href="#maxconcurrentpercentage" title="MaxConcurrentPercentage">MaxConcurrentPercentage</a>" : <i>Integer</i>,
    "<a href="#regionorder" title="RegionOrder">RegionOrder</a>" : <i>[ String, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#failuretolerancecount" title="FailureToleranceCount">FailureToleranceCount</a>: <i>Integer</i>
<a href="#failuretolerancepercentage" title="FailureTolerancePercentage">FailureTolerancePercentage</a>: <i>Integer</i>
<a href="#maxconcurrentcount" title="MaxConcurrentCount">MaxConcurrentCount</a>: <i>Integer</i>
<a href="#maxconcurrentpercentage" title="MaxConcurrentPercentage">MaxConcurrentPercentage</a>: <i>Integer</i>
<a href="#regionorder" title="RegionOrder">RegionOrder</a>: <i>
      - String</i>
</pre>

## Properties

#### FailureToleranceCount

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### FailureTolerancePercentage

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MaxConcurrentCount

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MaxConcurrentPercentage

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RegionOrder

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

