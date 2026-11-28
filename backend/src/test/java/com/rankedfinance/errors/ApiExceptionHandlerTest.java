package com.rankedfinance.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ApiExceptionHandlerTest.FailingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandlerTest.FailingController.class)
class ApiExceptionHandlerTest {

    record NameRequest(@NotBlank String name) {}

    @RestController
    static class FailingController {

        @GetMapping("/test/boom")
        String boom() {
            throw new RuntimeException("access-sandbox-token-123");
        }

        @PostMapping("/test/names")
        String create(@Valid @RequestBody NameRequest body) {
            return body.name();
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new ApiException(ErrorCode.CONFLICT, "x");
        }
    }

    @Autowired MockMvc mvc;

    @Test
    void unexpectedExceptionNeverLeaksItsMessage() throws Exception {
        String body =
                mvc.perform(get("/test/boom"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.error").value("internal_error"))
                        .andExpect(jsonPath("$.detail").value("Something went wrong."))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(body).doesNotContain("token");
    }

    @Test
    void invalidBodyListsTheFields() throws Exception {
        mvc.perform(
                        post("/test/names")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void wrongMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(delete("/test/names"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("method_not_allowed"));
    }

    @Test
    void apiExceptionUsesItsCode() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"))
                .andExpect(jsonPath("$.detail").value("x"));
    }
}
