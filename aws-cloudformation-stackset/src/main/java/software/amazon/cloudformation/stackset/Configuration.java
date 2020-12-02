package software.amazon.cloudformation.stackset;

import org.json.JSONObject;
import org.json.JSONTokener;
import software.amazon.awssdk.utils.CollectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-cloudformation-stackset.json");
    }

    public JSONObject resourceSchemaJsonObject() {
        return new JSONObject(
                new JSONTokener(this.getClass().getClassLoader().getResourceAsStream(schemaFilename)));
    }

    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        if (CollectionUtils.isNullOrEmpty(resourceModel.getTags())) return null;

        return resourceModel.getTags()
                .stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue, (value1, value2) -> value2));
    }
}
