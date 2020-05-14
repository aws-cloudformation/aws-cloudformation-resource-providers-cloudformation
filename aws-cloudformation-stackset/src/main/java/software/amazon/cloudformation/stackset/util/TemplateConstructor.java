package software.amazon.cloudformation.stackset.util;

import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.Arrays;
import java.util.List;

public final class TemplateConstructor extends SafeConstructor {

    private final static String FUNCTION_PREFIX = "Fn::";
    private final static List<String> FUNCTION_KEYS = Arrays.asList(
            "Fn::And", "Fn::Base64", "Condition", "Fn::Contains",
            "Fn::EachMemberEquals", "Fn::EachMemberIn", "Fn::Equals",
            "Fn::FindInMap", "Fn::GetAtt", "Fn::GetAZs", "Fn::If",
            "Fn::ImportValue", "Fn::Join", "Fn::Not", "Fn::Or",
            "Ref", "Fn::RefAll", "Fn::Select", "Fn::Split", "Fn::Sub",
            "Fn::ValueOf", "Fn::ValueOfAll", "Fn::Cidr");

    TemplateConstructor() {
        for (final String token : FUNCTION_KEYS) {
            this.yamlConstructors.put(new Tag("!" + stripFn(token)), new ConstructYamlStr());
        }
    }

    private static String stripFn(String keyword) {
        return keyword.startsWith(FUNCTION_PREFIX) ? keyword.substring(4) : keyword;
    }
}
