# AWS::CloudFormation::ModuleDefaultVersion


## Sample Usage

```
Resources:
    InitialModule:
        Type: AWS::CloudFormation::ModuleVersion
        Properties:
            ModuleName: Sample::CloudFormation::Resource::Module
            ModulePackage: s3://<your_s3_bucket_name>/<your_module_package_key>
    UpdatedModule:
        Type: AWS::CloudFormation::ModuleVersion
        Properties:
            ModuleName: Sample::CloudFormation::Resource::Module
            ModulePackage: s3://<your_s3_bucket_name>/<your_module_package_key>
        DependsOn: InitialModule

    DefaultVersion:
        Type: AWS::CloudFormation::ModuleDefaultVersion
        Properties:
            Arn: !Ref UpdatedModule
```
