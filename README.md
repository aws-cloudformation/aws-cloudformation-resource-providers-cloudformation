## AWS CloudFormation Resource Provider Package For AWS CloudFormation

This repository contains AWS-owned resource providers for the `AWS::CloudFormation::*` namespace.

Usage
-----

The CloudFormation CLI (cfn) allows you to author your own resource providers that can be used by CloudFormation.

Refer to the documentation for the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) for usage instructions.


Development
-----------

First, you will need to install the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk), as it is a required dependency:

```shell
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```

Linting and running unit tests is done via [pre-commit](https://pre-commit.com/), and so is performed automatically on commit. The continuous integration also runs these checks.

```shell
pre-commit install
```

Manual options are available so you don't have to commit:

```shell
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests and coverage checks
mvn verify
```

License
-------

This library is licensed under the Apache 2.0 License.
