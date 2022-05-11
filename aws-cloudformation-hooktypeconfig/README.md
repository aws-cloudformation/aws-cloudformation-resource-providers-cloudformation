# AWS::CloudFormation::HookTypeConfig


## Sample Usage

```
Resources:
    InitialConfig:
        Type: AWS::CloudFormation::HookTypeConfig
        Properties:
            TypeName: Sample::CloudFormation::Hook
            Configuration: {sample config}
    UpdatedModule:
        Type: AWS::CloudFormation::HookTypeConfig
        Properties:
            ModuleName: Sample::CloudFormation::Hook
            Configuration: {sample config}
        DependsOn: InitialConfig
```
