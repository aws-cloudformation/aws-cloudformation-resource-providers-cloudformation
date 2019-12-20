# AWS::CloudFormation::ResourceVersionAlias


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
        Type: AWS::CloudFormation::ResourceVersionAlias
        Properties:
            Arn: !Ref UpdatedType
            # DefaultVersionId: !GetAtt UpdatedType.VersionId
            DefaultVersionId: "00000002"
            TypeName: Sample::CloudFormation::Resource
```
