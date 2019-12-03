package software.amazon.cloudformation.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext {
    private boolean createStarted;
    private boolean createStabilized;
    private boolean deleteStarted;
    private String registrationToken;

    @JsonPOJOBuilder(withPrefix = "")
    public static class CallbackContextBuilder {
    }
}
