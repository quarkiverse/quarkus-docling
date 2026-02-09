package io.quarkiverse.docling.runtime.jackson;

import io.quarkus.jackson.JacksonMixin;

import ai.docling.serve.api.validation.ValidationErrorDetail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JacksonMixin(ValidationErrorDetail.Builder.class)
@JsonIgnoreProperties("input")
public interface ValidationErrorDetailBuilderMixin {
}
