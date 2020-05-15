package software.amazon.cloudformation.stackset.util;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import software.amazon.awssdk.utils.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
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

    private final Resolver resolver = new Resolver();

    TemplateConstructor() {
        this.yamlConstructors.put(Tag.TIMESTAMP, new ConstructYamlStr());
        for (final String token : FUNCTION_KEYS) {
            this.yamlConstructors.put(new Tag("!" + stripFn(token)), new AbstractConstruct() {

                @Override
                public Object construct(Node node) {
                    final LinkedHashMap<String, Object> retVal = new LinkedHashMap<>(2);
                    retVal.put(token, constructObject(getDelegateNode(node)));
                    return retVal;
                }

                private Node getDelegateNode(Node node) {
                    if (node instanceof ScalarNode) {
                        final Tag nodeTag;
                        String nodeValue = ((ScalarNode) node).getValue();

                        if (nodeValue != null && StringUtils.isEmpty(nodeValue)) {
                            nodeTag = Tag.STR;
                        } else {
                            nodeTag = resolver.resolve(NodeId.scalar, nodeValue, true);
                        }

                        return new ScalarNode(nodeTag, nodeValue, node.getStartMark(),
                                node.getEndMark(), ((ScalarNode) node).getScalarStyle());
                    }
                    if (node instanceof SequenceNode) {
                        return new SequenceNode(Tag.SEQ, true, ((SequenceNode) node).getValue(),
                                node.getStartMark(), node.getEndMark(), ((SequenceNode) node).getFlowStyle());
                    }
                    if (node instanceof MappingNode) {
                        return new MappingNode(Tag.MAP, true, ((MappingNode) node).getValue(),
                                node.getStartMark(), node.getEndMark(), ((MappingNode) node).getFlowStyle());
                    }
                    throw new ParseException("Invalid node type for tag " + node.getTag().toString());
                }
            });
        }
    }

    private static String stripFn(String keyword) {
        return keyword.startsWith(FUNCTION_PREFIX) ? keyword.substring(4) : keyword;
    }
}
