package software.amazon.cloudformation.stackset.util;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class to extract AWS Credentials Provider from {@link AmazonWebServicesClientProxy}.
 *
 * Because {@link AmazonWebServicesClientProxy#injectCredentialsAndInvokeV2(AwsRequest, Function)} doesn't extend
 * {@link ResponseInputStream<AwsResponse>}, but S3 GetObject requires AWS Credentials Provider to authenticate user,
 * we have to mimic dummy aws request, aws response and a function as input parameters to
 * {@link AmazonWebServicesClientProxy#injectCredentialsAndInvokeV2(AwsRequest, Function)} to obtain credentials.
 */
public final class AwsCredentialsExtractor {

    private static final String AWS_CREDENTIALS_NOT_AVAILABLE_ERROR_MSG = "AWS credentials provider are not available";

    /**
     * Function to extract Aws Credentials Provider from {@link AmazonWebServicesClientProxy}.
     *
     * @param proxy {@link AmazonWebServicesClientProxy}
     * @return {@link AwsCredentialsProvider}
     */
    public static AwsCredentialsProvider extractAwsCredentialsProvider(final AmazonWebServicesClientProxy proxy) {
        return proxy.injectCredentialsAndInvokeV2(
                GetAwsCredentialsRequest.builder().build(),
                AwsCredentialsExtractor::extract
        ).awsCredentialsProvider;
    }

    private static GetAwsCredentialsResponse extract(final GetAwsCredentialsRequest getAwsCredentialsRequest) {
        final AwsCredentialsProvider awsCredentialsProvider = getAwsCredentialsRequest.overrideConfiguration()
                .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                .orElseThrow(() -> new IllegalArgumentException(AWS_CREDENTIALS_NOT_AVAILABLE_ERROR_MSG));
        return GetAwsCredentialsResponse.builder().awsCredentialsProvider(awsCredentialsProvider).build();
    }

    /**
     * Inner class to mimic {@link AwsRequest}.
     * No additional input parameter is required. Other classes and functions are implemented by following interfaces
     * and abstract method of {@link AwsRequest}.
     */
    private final static class GetAwsCredentialsRequest extends AwsRequest
            implements ToCopyableBuilder<GetAwsCredentialsRequest.Builder, GetAwsCredentialsRequest> {

        private GetAwsCredentialsRequest(Builder builder) {
            super(builder);
        }

        static GetAwsCredentialsRequest.Builder builder() {
            return new GetAwsCredentialsRequest.BuilderImpl();
        }

        @Override
        public Builder toBuilder() {
            return new GetAwsCredentialsRequest.BuilderImpl();
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public boolean equalsBySdkFields(Object obj) {
            return true;
        }

        static final class BuilderImpl extends AwsRequest.BuilderImpl
                implements GetAwsCredentialsRequest.Builder {

            BuilderImpl() {
            }

            public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
                super.overrideConfiguration(overrideConfiguration);
                return this;
            }

            public GetAwsCredentialsRequest.Builder overrideConfiguration(
                    Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer
            ) {
                super.overrideConfiguration(builderConsumer);
                return this;
            }

            public GetAwsCredentialsRequest build() {
                return new GetAwsCredentialsRequest(this);
            }

            public List<SdkField<?>> sdkFields() {
                return Collections.emptyList();
            }
        }

        public interface Builder
                extends AwsRequest.Builder, SdkPojo, CopyableBuilder<Builder, GetAwsCredentialsRequest> {
            @Override
            GetAwsCredentialsRequest.Builder overrideConfiguration(
                    AwsRequestOverrideConfiguration awsRequestOverrideConfiguration
            );

            @Override
            GetAwsCredentialsRequest.Builder overrideConfiguration(
                    Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer
            );
        }
    }

    /**
     * Inner class to mimic {@link AwsResponse} in order to obtain credentials from
     * {@link AmazonWebServicesClientProxy}.
     *
     * {@link AwsCredentialsProvider} is the additional parameter in this class. Other classes and functions are
     * implemented by following interfaces and abstract method of {@link AwsResponse}.
     */
    private static class GetAwsCredentialsResponse extends AwsResponse
            implements ToCopyableBuilder<GetAwsCredentialsResponse.Builder, GetAwsCredentialsResponse> {

        private final GetAwsCredentialsResponseMetadata responseMetadata;

        private final AwsCredentialsProvider awsCredentialsProvider;

        private GetAwsCredentialsResponse(final GetAwsCredentialsResponse.BuilderImpl builder) {
            super(builder);
            this.awsCredentialsProvider = (builder.awsCredentialsProvider);
            this.responseMetadata = builder.responseMetadata();
        }

        public AwsCredentialsProvider awsCredentialsProvider() {
            return this.awsCredentialsProvider;
        }

        @Override
        public Builder toBuilder() {
            return new GetAwsCredentialsResponse.BuilderImpl(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public boolean equalsBySdkFields(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof GetAwsCredentialsResponse)) {
                return false;
            } else {
                GetAwsCredentialsResponse other = (GetAwsCredentialsResponse) obj;
                return Objects.equals(this.awsCredentialsProvider(), other.awsCredentialsProvider());
            }
        }

        public static GetAwsCredentialsResponse.Builder builder() {
            return new GetAwsCredentialsResponse.BuilderImpl();
        }

        static final class BuilderImpl extends AwsResponse.BuilderImpl
                implements GetAwsCredentialsResponse.Builder {

            private GetAwsCredentialsResponseMetadata responseMetadata;

            private AwsCredentialsProvider awsCredentialsProvider;

            private BuilderImpl() {
            }

            private BuilderImpl(GetAwsCredentialsResponse response) {
                super(response);
                this.awsCredentialsProvider = response.awsCredentialsProvider;
            }

            public GetAwsCredentialsResponse build() {
                return new GetAwsCredentialsResponse(this);
            }

            public List<SdkField<?>> sdkFields() {
                return Collections.emptyList();
            }

            public GetAwsCredentialsResponseMetadata responseMetadata() {
                return this.responseMetadata;
            }

            public GetAwsCredentialsResponse.Builder responseMetadata(AwsResponseMetadata responseMetadata) {
                this.responseMetadata = GetAwsCredentialsResponseMetadata.create(responseMetadata);
                return this;
            }

            public final GetAwsCredentialsResponse.Builder awsCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider) {
                this.awsCredentialsProvider = awsCredentialsProvider;
                return this;
            }

            public AwsCredentialsProvider getAwsCredentialsProvider() {
                return awsCredentialsProvider;
            }

            public void setAwsCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider) {
                this.awsCredentialsProvider = awsCredentialsProvider;
            }
        }

        public interface Builder extends AwsResponse.Builder, SdkPojo,
                CopyableBuilder<Builder, GetAwsCredentialsResponse> {

            GetAwsCredentialsResponse build();

            GetAwsCredentialsResponseMetadata responseMetadata();

            GetAwsCredentialsResponse.Builder responseMetadata(AwsResponseMetadata awsResponseMetadata);

            GetAwsCredentialsResponse.Builder awsCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider);
        }
    }

    /**
     * Inner class to mimic {@link AwsResponseMetadata} which is required by {@link AwsResponse}.
     */
    private static final class GetAwsCredentialsResponseMetadata extends AwsResponseMetadata {
        private GetAwsCredentialsResponseMetadata(AwsResponseMetadata responseMetadata) {
            super(responseMetadata);
        }

        public static GetAwsCredentialsResponseMetadata create(AwsResponseMetadata responseMetadata) {
            return new GetAwsCredentialsResponseMetadata(responseMetadata);
        }
    }
}
