# AWS::CloudFormation::ResourceDefaultVersion


## Sample Usage

```
Resources:
    InitialType:
        Type: AWS::CloudFormation::ResourceVersion
        Properties:
            TypeName: Sample::CloudFormation::Resource
            SchemaHandlerPackage: s3://cloudformationmanageduploadinfrast-artifactbucket-123456789012abcdef/sample-cloudformation-resource.zip
    UpdatedType:
        Type: AWS::CloudFormation::ResourceVersion
        Properties:
            TypeName: Sample::CloudFormation::Resource
            SchemaHandlerPackage: s3://cloudformationmanageduploadinfrast-artifactbucket-123456789012abcdef/sample-cloudformation-resource-update.zip
        DependsOn: InitialType

    DefaultVersion:
        Type: AWS::CloudFormation::ResourceDefaultVersion
        Properties:
            TypeVersionArn: !Ref UpdatedType
```
