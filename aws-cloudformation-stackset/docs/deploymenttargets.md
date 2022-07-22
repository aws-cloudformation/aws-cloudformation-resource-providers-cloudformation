# AWS::CloudFormation::StackSet DeploymentTargets

 The AWS OrganizationalUnitIds or Accounts for which to create stack instances in the specified Regions.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#accounts" title="Accounts">Accounts</a>" : <i>[ String, ... ]</i>,
    "<a href="#organizationalunitids" title="OrganizationalUnitIds">OrganizationalUnitIds</a>" : <i>[ String, ... ]</i>,
    "<a href="#accountfiltertype" title="AccountFilterType">AccountFilterType</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#accounts" title="Accounts">Accounts</a>: <i>
      - String</i>
<a href="#organizationalunitids" title="OrganizationalUnitIds">OrganizationalUnitIds</a>: <i>
      - String</i>
<a href="#accountfiltertype" title="AccountFilterType">AccountFilterType</a>: <i>String</i>
</pre>

## Properties

#### Accounts

AWS accounts that you want to create stack instances in the specified Region(s) for.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OrganizationalUnitIds

The organization root ID or organizational unit (OU) IDs to which StackSets deploys.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AccountFilterType

The filter type you want to apply on organizational units and accounts.

_Required_: No

_Type_: String

_Allowed Values_: <code>NONE</code> | <code>UNION</code> | <code>INTERSECTION</code> | <code>DIFFERENCE</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
