# PGE::CloudFormation::StackSet StackInstances

Stack instances in some specific accounts and Regions.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#deploymenttargets" title="DeploymentTargets">DeploymentTargets</a>" : <i><a href="deploymenttargets.md">DeploymentTargets</a></i>,
    "<a href="#regions" title="Regions">Regions</a>" : <i>[ String, ... ]</i>,
    "<a href="#parameteroverrides" title="ParameterOverrides">ParameterOverrides</a>" : <i>[ <a href="parameter.md">Parameter</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#deploymenttargets" title="DeploymentTargets">DeploymentTargets</a>: <i><a href="deploymenttargets.md">DeploymentTargets</a></i>
<a href="#regions" title="Regions">Regions</a>: <i>
      - String</i>
<a href="#parameteroverrides" title="ParameterOverrides">ParameterOverrides</a>: <i>
      - <a href="parameter.md">Parameter</a></i>
</pre>

## Properties

#### DeploymentTargets

 The AWS OrganizationalUnitIds or Accounts for which to create stack instances in the specified Regions.

_Required_: Yes

_Type_: <a href="deploymenttargets.md">DeploymentTargets</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Regions

The names of one or more Regions where you want to create stack instances using the specified AWS account(s).

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ParameterOverrides

A list of stack set parameters whose values you want to override in the selected stack instances.

_Required_: No

_Type_: List of <a href="parameter.md">Parameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

