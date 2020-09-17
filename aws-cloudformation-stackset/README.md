# PGE::CloudFormation::StackSet

Congratulations on starting development! Next steps:

1. Write the JSON schema describing your resource, `aws-cloudformation-stackset.json`
1. Implement your resource handlers.

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/) to enable auto-complete for Lombok-annotated classes.

Environment
```
virtualenv -p /usr/local/bin/python3 .venv_cfnss
source .venv_cfnss/bin/activate
pip3 install pipenv
pipenv --three install
jenv local 1.8
$ cat ~/.mavenrc
MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
pre-commit install
cfn init
```

Code Analysis
```
pylint --rcfile ~/pylintrc *py
aws cloudformation validate-template --template-body file://test-config-ec2-stackset.yml --profile PGE-Master-Dev
```

Code Generation
```
cfn generate
```

Package
```
mvn package
```

Test
```
pre-commit run --all-files
mvn verify
sam local start-lambda
cfn test
```

Deployment
```
export AWS_PROFILE=PGE-Master-Dev
cfn submit --set-default -v
```

Test an end-user template
```
aws cloudformation validate-template --template-url https://cf-templates-681v3yr6sl6i-us-west-2.s3-us-west-2.amazonaws.com/test-config-ec2-stackset.yml --profile PGE-Master-Dev
```
