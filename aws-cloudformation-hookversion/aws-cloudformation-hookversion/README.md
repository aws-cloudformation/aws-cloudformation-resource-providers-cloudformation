# AWS::CloudFormation::HookVersion


## Sample Usage

```
Resources:
    InitialType:
        Type: AWS::CloudFormation::HookVersion
        Properties:
            TypeName: Sample::CloudFormation::Hook
            SchemaHandlerPackage: s3://cloudformationmanageduploadinfrast-artifactbucket-123456789012abcdef/sample-cloudformation-hook.zip
    UpdatedType:
        Type: AWS::CloudFormation::HookVersion
        Properties:
            TypeName: Sample::CloudFormation::Hook
            SchemaHandlerPackage: s3://cloudformationmanageduploadinfrast-artifactbucket-123456789012abcdef/sample-cloudformation-hook-update.zip
        DependsOn: InitialType

    DefaultVersion:
        Type: AWS::CloudFormation::HookDefaultVersion
        Properties:
            TypeVersionArn: !Ref UpdatedType
```
