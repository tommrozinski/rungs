package com.rungs.readiness.adapter.in.web.api;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Strict JSON binding for request bodies (story 007, design §2). Boot's own customizer turns unknown-property failures
 * off, and this one runs after it (unordered beans have the lowest precedence), so:
 *
 * <ul>
 *   <li>unknown properties, including a misspelt field or a client-sent debt {@code id}, are rejected;
 *   <li>JSON numbers and booleans are not coerced into {@code String} fields, so {@code 1000} is not read as
 *       {@code "1000"};
 *   <li>JSON floats are not coerced into whole-number fields, so {@code 3.5} or {@code 4.0} is not read as a month
 *       count;
 *   <li>JSON strings are not coerced into whole-number fields, so {@code "3"} is not read as a month count (story 011).
 * </ul>
 *
 * <p>Each refusal surfaces as {@code INVALID_TYPE} through {@link ProblemDetailsAdvice}.
 */
@Configuration(proxyBeanMethods = false)
class ApiJsonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer strictRequestBinding() {
        return builder -> builder
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .withCoercionConfig(LogicalType.Textual, text -> text
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Integer, whole -> whole
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail));
    }
}
