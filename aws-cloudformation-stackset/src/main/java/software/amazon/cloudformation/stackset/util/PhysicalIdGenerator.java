package software.amazon.cloudformation.stackset.util;

import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;
import software.amazon.cloudformation.stackset.ResourceModel;

/**
 * Utility class to generate Physical Resource Id from {@link ResourceHandlerRequest<ResourceModel>}.
 */
public class PhysicalIdGenerator {

    private static int MAX_LENGTH_CALLER_REFERENCE = 128;

    /**
     * Generates a physical Id for creating a new resource.
     * @param request CloudFormation's requested resource state.
     * @return Physical ID.
     */
    public static String generatePhysicalId(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_LENGTH_CALLER_REFERENCE);
    }
}
