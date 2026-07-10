package cleanloop.common.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 캐치올 핸들러가 Spring이 이미 상태를 정한 예외를 500으로 뭉개지 않는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 매핑되지_않은_경로는_404다() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 지원하지_않는_메서드는_405다() throws Exception {
        mockMvc.perform(delete("/api/v1/me"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 지원하지_않는_컨텐츠_타입은_415다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void 깨진_JSON_본문은_400이다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 잘못된_형식의_경로_변수는_400이고_문제_필드를_알려준다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cycleDays\": 7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.categoryId").exists());
    }

    /**
     * Spring 예외 원문에는 내부 타입명이 섞여 있어 클라이언트에 노출하지 않는다.
     */
    @Test
    void 에러_메시지에_내부_타입명이_새지_않는다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cycleDays\": 7}"))
                .andExpect(jsonPath("$.error.message").value(not(containsString("java.lang"))))
                .andExpect(jsonPath("$.error.message").value(not(containsString("java.util.UUID"))));

        mockMvc.perform(delete("/api/v1/me"))
                .andExpect(jsonPath("$.error.message").value(not(containsString("Request method"))));

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(jsonPath("$.error.message").value(not(containsString("Content-Type 'text/plain'"))));
    }

    @Test
    void 에러_응답에도_requestId가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }
}
