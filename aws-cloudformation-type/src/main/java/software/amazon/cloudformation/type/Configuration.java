package software.amazon.cloudformation.type;

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-cloudformation-type.json");
    }
}
