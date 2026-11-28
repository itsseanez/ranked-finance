package com.rankedfinance.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.rankedfinance.TestcontainersConfiguration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// The generated TypeScript types come from this spec, so the frontend's message map can't miss a
// code the backend sends.
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ErrorCodeContractTest {

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    @Test
    void everyCodeIsInTheOpenApiSpec() throws Exception {
        String spec =
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode codes =
                json.readTree(spec).at("/components/schemas/ApiProblem/properties/error/enum");

        List<String> inSpec = codes.valueStream().map(JsonNode::asString).toList();
        assertThat(inSpec)
                .containsExactlyElementsOf(
                        Arrays.stream(ErrorCode.values()).map(ErrorCode::code).toList());
    }

    @Test
    void everyOperationAnswersFailuresWithTheProblemSchema() throws Exception {
        String spec =
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode me = json.readTree(spec).at("/paths/~1api~1v1~1me/get/responses");

        for (String range : List.of("4XX", "5XX")) {
            assertThat(
                            me.at("/" + range + "/content/application~1problem+json/schema/$ref")
                                    .asString())
                    .isEqualTo("#/components/schemas/ApiProblem");
        }
    }

    // CurrentUser comes from the session; it must not show up as a request parameter.
    @Test
    void currentUserIsNotAParameter() throws Exception {
        String spec =
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(spec).at("/paths/~1api~1v1~1me/get/parameters").isMissingNode())
                .isTrue();
    }
}
