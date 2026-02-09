package io.quarkiverse.docling.runtime.jackson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ai.docling.serve.api.validation.ValidationErrorDetail;
import io.quarkus.jackson.JacksonMixin;

@JacksonMixin(ValidationErrorDetail.Builder.class)
@JsonIgnoreProperties("input")
public interface ValidationErrorDetailBuilderMixin {
}
