package software.amazon.cloudformation.stackset.util;

import com.google.common.base.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.amazon.cloudformation.stackset.util.TestUtils.INVALID_EMBEDDED_STACKSET_TEMPLATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.INVALID_EMBEDDED_STACK_TEMPLATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_BODY;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_URL;
import static software.amazon.cloudformation.stackset.util.TestUtils.VALID_YAML_SHORTHANDS_TEMPLATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.VALID_YAML_TEMPLATE;

@ExtendWith(MockitoExtension.class)
public class ValidatorTest {

    private static final List<String> INVALID_S3_URLS = Arrays.asList(
            "http://s3-us-west-2.amazonaws.com//object.json", "nhttp://s3-us-west-2.amazonaws.com/test/",
            "invalid_url", "http://s3-us-west-2.amazonaws.com");

    private static final long VALID_TEMPLATE_SIZE = 1000L;
    private static final long INVALID_TEMPLATE_SIZE = 460801L;

    @Spy
    private Validator validator;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        validator = spy(Validator.class);
    }

    @Test
    public void testValidateTemplate_InvalidUri() {
        for (final String invalidS3Url : INVALID_S3_URLS) {
            assertThrows(CfnInvalidRequestException.class,
                    () -> validator.validateTemplate(proxy, null, invalidS3Url, logger));
        }
    }

    @Test
    public void testValidateTemplate_BothBodyAndUriExist() {
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, TEMPLATE_BODY, TEMPLATE_URL, logger));
    }

    @Test
    public void testGetUrlContent() {
        final ResponseBytes<?> responseBytes = mock(ResponseBytes.class);
        doReturn(HeadObjectResponse.builder().contentLength(VALID_TEMPLATE_SIZE).build())
                .when(proxy).injectCredentialsAndInvokeV2(any(), any());
        doReturn(ResponseBytes.fromByteArray(responseBytes, TEMPLATE_BODY.getBytes())).when(proxy)
                .injectCredentialsAndInvokeV2Bytes(any(), any());
        assertEquals(validator.getUrlContent(proxy, TEMPLATE_URL), TEMPLATE_BODY);
    }

    @Test
    public void testGetUrlContent_TemplateTooLarge() {
        doReturn(HeadObjectResponse.builder().contentLength(INVALID_TEMPLATE_SIZE).build())
                .when(proxy).injectCredentialsAndInvokeV2(any(), any());
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.getUrlContent(proxy, TEMPLATE_URL));
    }

    @Test
    public void testValidateTemplate_BothBodyAndUriNotExist() {
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, null, null, logger));
    }

    @Test
    public void testValidateTemplate_InvalidTemplate() {
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, INVALID_EMBEDDED_STACK_TEMPLATE, null, logger));
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, INVALID_EMBEDDED_STACKSET_TEMPLATE, null, logger));
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, "", null, logger));
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, "null", null, logger));
    }

    @Test
    public void testValidateTemplate_ValidTemplateBody() {
        assertDoesNotThrow(() -> validator.validateTemplate(proxy, TEMPLATE_BODY, null, logger));
        assertDoesNotThrow(() -> validator.validateTemplate(proxy, VALID_YAML_TEMPLATE, null, logger));
        assertDoesNotThrow(() -> validator.validateTemplate(proxy, VALID_YAML_SHORTHANDS_TEMPLATE, null, logger));
    }
}
