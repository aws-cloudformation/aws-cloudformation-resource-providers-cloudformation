package software.amazon.cloudformation.stackset.util;

import com.amazonaws.util.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_BODY;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_URL;

@ExtendWith(MockitoExtension.class)
public class ValidatorTest {

    private static final String TEMPLATES_PATH_PREFIX = "/java/resources/";

    private static final List<String> INVALID_TEMPLATE_FILENAMES = Arrays.asList(
            "nested_stack.json", "nested_stackset.json", "invalid_format.json",
            "invalid_format.yaml");

    private static final List<String> VALID_TEMPLATE_FILENAMES = Arrays.asList(
            "valid.json", "valid.yaml");

    private static final List<String> INVALID_S3_URLS = Arrays.asList(
            "http://s3-us-west-2.amazonaws.com//object.json", "nhttp://s3-us-west-2.amazonaws.com/test/",
            "invalid_url", "http://s3-us-west-2.amazonaws.com");

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
    public void testValidateTemplate_InvalidFormatError() {
        for (final String filename : INVALID_TEMPLATE_FILENAMES) {
            doReturn(read(TEMPLATES_PATH_PREFIX + filename)).when(validator).getUrlContent(any(), any());
            assertThrows(CfnInvalidRequestException.class,
                    () -> validator.validateTemplate(proxy, null, TEMPLATE_URL, logger));
        }
    }

    @Test
    public void testValidateTemplate_ValidS3Format() {
        for (final String filename : VALID_TEMPLATE_FILENAMES) {
            doReturn(read(TEMPLATES_PATH_PREFIX + filename)).when(validator).getUrlContent(any(), any());
            assertDoesNotThrow(() -> validator.validateTemplate(proxy, null, TEMPLATE_URL, logger));
        }
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
    public void testValidateTemplate_BothBodyAndUriNotExist() {
        assertThrows(CfnInvalidRequestException.class,
                () -> validator.validateTemplate(proxy, null, null, logger));
    }

    @Test
    public void testValidateTemplate_ValidTemplateBody() {
        assertDoesNotThrow(() -> validator.validateTemplate(proxy, TEMPLATE_BODY, null, logger));
    }

    public String read(final String fileName) {
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}