package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.HeaderParam;

import org.jspecify.annotations.Nullable;

import ai.docling.serve.api.util.ValidationUtils;

public final class ApiMetadata {
    /**
     * Represents the name of the HTTP header used for passing the API key.
     * This constant is typically used when authenticating requests to the API endpoints
     * by including the API key in the specified header field.
     */
    public static final String API_KEY_HEADER_NAME = "X-Api-Key";

    @HeaderParam(API_KEY_HEADER_NAME)
    @Nullable
    public final String apiKey;

    private ApiMetadata(Builder builder) {
        this.apiKey = builder.apiKey;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nullable
        private String apiKey;

        private Builder() {
        }

        private Builder(ApiMetadata apiMetadata) {
            this.apiKey = ValidationUtils.ensureNotNull(apiMetadata, "apiMetadata").apiKey;
        }

        public Builder apiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public ApiMetadata build() {
            return new ApiMetadata(this);
        }
    }
}
