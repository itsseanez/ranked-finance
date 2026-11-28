package com.rankedfinance.errors;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Puts the error shape and every {@link ErrorCode} in the OpenAPI spec, so `npm run gen:api` gives
 * the frontend a typed list of codes. An OpenApiCustomizer is a springdoc hook that edits the spec
 * after it's generated from the controllers.
 */
@Component
class ErrorCodesOpenApi implements OpenApiCustomizer {

    static final String SCHEMA = "ApiProblem";

    @Override
    public void customise(io.swagger.v3.oas.models.OpenAPI openApi) {
        openApi.getComponents().addSchemas(SCHEMA, problemSchema());
        Content problem =
                new Content()
                        .addMediaType(
                                org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                new MediaType()
                                        .schema(
                                                new Schema<>()
                                                        .$ref("#/components/schemas/" + SCHEMA)));
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .forEach(
                        operation -> {
                            // Replace whatever springdoc guessed per status with the one shape.
                            operation
                                    .getResponses()
                                    .keySet()
                                    .removeIf(
                                            status ->
                                                    status.startsWith("4")
                                                            || status.startsWith("5"));
                            operation
                                    .getResponses()
                                    .addApiResponse(
                                            "4XX",
                                            new ApiResponse()
                                                    .description("Client error")
                                                    .content(problem))
                                    .addApiResponse(
                                            "5XX",
                                            new ApiResponse()
                                                    .description("Server error")
                                                    .content(problem));
                        });
    }

    private static Schema<?> problemSchema() {
        StringSchema code = new StringSchema();
        code.setEnum(Arrays.stream(ErrorCode.values()).map(ErrorCode::code).toList());
        code.description("Machine-readable code; clients must handle codes they don't know.");
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("field", new StringSchema());
        fieldError.addProperty("message", new StringSchema());
        fieldError.setRequired(List.of("field", "message"));
        ObjectSchema schema = new ObjectSchema();
        schema.description("An RFC 9457 problem detail with an error code (ADR 0014).");
        schema.addProperty("type", new StringSchema().format("uri"));
        schema.addProperty("title", new StringSchema());
        schema.addProperty("status", new IntegerSchema().format("int32"));
        schema.addProperty("detail", new StringSchema());
        schema.addProperty("error", code);
        schema.addProperty("errors", new ArraySchema().items(fieldError));
        schema.setRequired(List.of("status", "error"));
        return schema;
    }
}
