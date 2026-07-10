package cleanloop.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * docs/issues/03-completion.md 완료 기준 검증.
 * 시드: 완료 기록 10건, 욕실 카테고리(cycle 14, 16일 전 완료 -> due).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompletionApiTest {

    private static final String BATH_ID = "b0000000-0000-0000-0000-000000000001";
    private static final String USER_ID = "a0000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 완료하면_로그가_생기고_lastDoneAt이_갱신된다() throws Exception {
        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completedAt": "2026-07-10T12:00:00+09:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category.id").value(BATH_ID))
                .andExpect(jsonPath("$.data.category.lastDoneAt").value(Matchers.startsWith("2026-07-10T12:00")))
                .andExpect(jsonPath("$.data.category.nextDueAt").value(Matchers.startsWith("2026-07-24T12:00")))
                .andExpect(jsonPath("$.data.log.categoryId").value(BATH_ID))
                .andExpect(jsonPath("$.data.log.categoryName").value("욕실"))
                .andExpect(jsonPath("$.data.log.completedAt").value(Matchers.startsWith("2026-07-10T12:00")))
                .andExpect(jsonPath("$.data.toastMessage").value("욕실 완료. 다음 관리는 7월 24일에 보면 충분해요."));
    }

    @Test
    void 완료_후_카테고리_상태가_재계산된다() throws Exception {
        // 완료 전 욕실은 due (16일 전 완료, 주기 14일)
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data[0].status.code").value("due"));

        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category.status.code").value("good"))
                .andExpect(jsonPath("$.data.category.status.daysUntilNext").value(14));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data[0].status.code").value("good"));
    }

    @Test
    void completedAt을_생략하면_현재_시각으로_완료한다() throws Exception {
        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.log.completedAt").exists());
    }

    @Test
    void 완료_기록이_실제로_저장된다() throws Exception {
        int before = countLogs();

        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete"))
                .andExpect(status().isCreated());

        assertThat(countLogs()).isEqualTo(before + 1);
    }

    @Test
    void 같은_카테고리를_같은_날_여러_번_완료할_수_있다() throws Exception {
        int before = countLogs();

        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete")).andExpect(status().isCreated());

        assertThat(countLogs()).isEqualTo(before + 2);
    }

    @Test
    void 없는_카테고리를_완료하면_404다() throws Exception {
        mockMvc.perform(post("/api/v1/categories/b0000000-0000-0000-0000-0000000000ff/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void 완료에_실패하면_로그가_남지_않는다() throws Exception {
        int before = countLogs();

        mockMvc.perform(post("/api/v1/categories/b0000000-0000-0000-0000-0000000000ff/complete"))
                .andExpect(status().isNotFound());

        assertThat(countLogs()).isEqualTo(before);
    }

    @Test
    void 완료_기록을_최신순으로_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/completion-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(10)))
                // 시드에서 가장 최근은 1일 전 완료한 바닥/먼지
                .andExpect(jsonPath("$.data[0].categoryName").value("바닥/먼지"))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist());
    }

    @Test
    void 완료_기록은_완료_시각_내림차순이다() throws Exception {
        JsonNode logs = readData(mockMvc.perform(get("/api/v1/completion-logs"))
                .andReturn().getResponse().getContentAsString());

        for (int i = 1; i < logs.size(); i++) {
            String previous = logs.get(i - 1).get("completedAt").asText();
            String current = logs.get(i).get("completedAt").asText();
            assertThat(previous).isGreaterThanOrEqualTo(current);
        }
    }

    @Test
    void limit으로_페이지_크기를_제한하고_커서를_내려준다() throws Exception {
        mockMvc.perform(get("/api/v1/completion-logs").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty());
    }

    @Test
    void 커서로_다음_페이지를_이어_읽으면_중복과_누락이_없다() throws Exception {
        var collected = new java.util.ArrayList<String>();
        String cursor = null;

        do {
            var request = get("/api/v1/completion-logs").param("limit", "3");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            String body = mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (JsonNode log : readData(body)) {
                collected.add(log.get("id").asText());
            }
            JsonNode next = objectMapper.readTree(body).path("meta").path("nextCursor");
            cursor = next.isNull() || next.isMissingNode() ? null : next.asText();
        } while (cursor != null);

        assertThat(collected).hasSize(10);
        assertThat(collected).doesNotHaveDuplicates();
    }

    @Test
    void from과_to로_기간을_좁힌다() throws Exception {
        // to는 그 날 하루를 포함한다. 시드의 최신 기록은 1일 전이므로 오늘까지 조회하면 모두 나온다
        String today = java.time.LocalDate.now().toString();
        String threeDaysAgo = java.time.LocalDate.now().minusDays(3).toString();

        mockMvc.perform(get("/api/v1/completion-logs").param("from", threeDaysAgo).param("to", today))
                .andExpect(status().isOk())
                // 3일 전 세탁, 2일 전 쓰레기, 1일 전 바닥
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void 잘못된_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/completion-logs").param("cursor", "!!not-base64!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURSOR"));
    }

    @Test
    void base64지만_형식이_틀린_커서도_400이다() throws Exception {
        String malformed = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("nonsense".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/completion-logs").param("cursor", malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURSOR"));
    }

    @Test
    void limit이_허용_범위를_벗어나면_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/completion-logs").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/completion-logs").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode readData(String body) throws Exception {
        return objectMapper.readTree(body).get("data");
    }

    private int countLogs() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from completion_logs where user_id = ?", Integer.class, UUID.fromString(USER_ID));
        return count != null ? count : 0;
    }
}
