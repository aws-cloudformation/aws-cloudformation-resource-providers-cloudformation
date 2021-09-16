package software.amazon.cloudformation.stackv2;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-cloudformation-stackv2.json");
    }

    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        if (resourceModel.getTags() == null) {
            return null;
        } else {
            return resourceModel.getTags().stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue, (value1, value2) -> value2));
        }
    }
}
