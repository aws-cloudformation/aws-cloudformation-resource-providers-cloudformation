package software.amazon.cloudformation.typeversion;

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-cloudformation-typeversion.json");
    }
}
