package cleanloop.user;

import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * docs/issues/01-user.md 완료 기준 검증.
 * 데모 사용자 row를 수정하는 테스트가 있으므로 클래스 단위로 롤백한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void GET_me는_고정_데모_사용자를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("a0000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.data.name").value("보송"))
                .andExpect(jsonPath("$.data.avatarText").value("보"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void createdAt은_사용자_타임존_오프셋이_붙은_ISO_8601이다() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+09:00")));
    }

    @Test
    void PATCH_me로_이름과_아바타_문자를_수정한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "김보송", "avatarText": "송"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김보송"))
                .andExpect(jsonPath("$.data.avatarText").value("송"));

        // 수정 결과가 저장되어 다음 조회에 반영된다
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(jsonPath("$.data.name").value("김보송"))
                .andExpect(jsonPath("$.data.avatarText").value("송"));
    }

    @Test
    void PATCH_me에서_생략한_필드는_기존_값을_유지한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "루틴"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("루틴"))
                .andExpect(jsonPath("$.data.avatarText").value("보"));
    }

    @Test
    void 이름이_40자를_넘으면_422로_거부된다() throws Exception {
        String tooLong = "가".repeat(41);

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(tooLong)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.name").exists());
    }

    @Test
    void 빈_이름은_422로_거부된다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 아바타_문자가_4자를_넘으면_422로_거부된다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"avatarText": "보송보송보"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.avatarText").exists());
    }
}
