package cleanloop.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * docs/issues/04-home-summary.md 완료 기준 검증.
 * 시드: 활성 카테고리 6개, 완료 기록 10건(최근 40일 분산), 욕실은 due.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SummaryApiTest {

    private static final String BATH_ID = "b0000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 홈은_오늘_날짜와_메시지와_이번달_완료수를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.monthlyCompletionCount").isNumber());
    }

    @Test
    void 홈의_카테고리는_활성만_상태와_함께_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(jsonPath("$.data.categories", Matchers.hasSize(6)))
                .andExpect(jsonPath("$.data.categories[0].name").value("욕실"))
                .andExpect(jsonPath("$.data.categories[0].icon").value("bath"))
                .andExpect(jsonPath("$.data.categories[0].note").exists())
                .andExpect(jsonPath("$.data.categories[0].nextDueAt").exists())
                .andExpect(jsonPath("$.data.categories[0].status.code").value("due"));
    }

    /** 욕실이 due 상태이므로 오늘 챙기라는 문구가 나온다. */
    @Test
    void 챙길_카테고리가_있으면_그에_맞는_메시지를_준다() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(jsonPath("$.data.message").value("오늘 청소부터 챙겨요."));
    }

    @Test
    void 홈의_최근_기록은_최신순_다섯건이다() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(jsonPath("$.data.recentLogs", Matchers.hasSize(5)))
                .andExpect(jsonPath("$.data.recentLogs[0].categoryName").value("바닥/먼지"));
    }

    @Test
    void today를_주면_그_날짜_기준으로_상태를_계산한다() throws Exception {
        // 욕실은 16일 전 완료(주기 14일)라 오늘은 due. 20일 전으로 거슬러 보면 아직 여유가 있다
        String twentyDaysAgo = LocalDate.now().minusDays(20).toString();

        mockMvc.perform(get("/api/v1/home").param("today", twentyDaysAgo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today").value(twentyDaysAgo))
                .andExpect(jsonPath("$.data.categories[0].status.code").value("good"));
    }

    @Test
    void today를_오늘로_명시해도_생략했을_때와_같다() throws Exception {
        String withParam = mockMvc.perform(get("/api/v1/home").param("today", LocalDate.now().toString()))
                .andReturn().getResponse().getContentAsString();
        String withoutParam = mockMvc.perform(get("/api/v1/home"))
                .andReturn().getResponse().getContentAsString();

        assertThat(statusCodesOf(withParam)).isEqualTo(statusCodesOf(withoutParam));
    }

    @Test
    void 잘못된_today_형식은_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/home").param("today", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 완료하면_홈의_이번달_완료수가_증가한다() throws Exception {
        int before = monthlyCompletionCount();

        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete"))
                .andExpect(status().isCreated());

        assertThat(monthlyCompletionCount()).isEqualTo(before + 1);
    }

    @Test
    void 마이_요약은_프로필과_통계를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.id").value("a0000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.data.profile.name").value("보송"))
                .andExpect(jsonPath("$.data.profile.avatarText").value("보"))
                .andExpect(jsonPath("$.data.stats.categoryCount").value(6))
                .andExpect(jsonPath("$.data.stats.monthlyCompletionCount").isNumber());
    }

    @Test
    void 마이_요약의_저장_셀렉션은_아직_비어_있다() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(jsonPath("$.data.stats.savedSelectionCount").value(0))
                .andExpect(jsonPath("$.data.savedSelections", Matchers.hasSize(0)));
    }

    @Test
    void 마이_요약의_최근_기록은_최신순_다섯건이다() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(jsonPath("$.data.recentLogs", Matchers.hasSize(5)))
                .andExpect(jsonPath("$.data.recentLogs[0].categoryName").value("바닥/먼지"));
    }

    @Test
    void 주간_발자국은_항상_12주를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(jsonPath("$.data.weeklyFootprints", Matchers.hasSize(12)));
    }

    @Test
    void 주간_발자국은_월요일_시작이고_오름차순이다() throws Exception {
        JsonNode footprints = readMeSummary().get("weeklyFootprints");

        LocalDate expectedFirst = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(11);

        for (int week = 0; week < footprints.size(); week++) {
            LocalDate weekStart = LocalDate.parse(footprints.get(week).get("weekStartDate").asText());
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(weekStart).isEqualTo(expectedFirst.plusWeeks(week));
        }
    }

    @Test
    void 기록이_없는_주도_0건으로_채운다() throws Exception {
        JsonNode footprints = readMeSummary().get("weeklyFootprints");

        // 시드 기록은 최근 40일 안에만 있으므로 앞쪽 주들은 0건이다
        assertThat(footprints.get(0).get("completionCount").asInt()).isZero();
        assertThat(footprints.get(0).get("level").asInt()).isZero();
    }

    @Test
    void 주간_발자국의_합계는_최근_12주_완료수와_맞는다() throws Exception {
        JsonNode footprints = readMeSummary().get("weeklyFootprints");

        int total = 0;
        for (JsonNode footprint : footprints) {
            total += footprint.get("completionCount").asInt();
        }
        // 시드 10건 중 40일 전 기록은 12주 창 안에 있다
        assertThat(total).isEqualTo(10);
    }

    @Test
    void 완료하면_이번_주_발자국이_증가한다() throws Exception {
        int before = currentWeekCompletionCount();

        mockMvc.perform(post("/api/v1/categories/" + BATH_ID + "/complete"))
                .andExpect(status().isCreated());

        assertThat(currentWeekCompletionCount()).isEqualTo(before + 1);
    }

    private int currentWeekCompletionCount() throws Exception {
        JsonNode footprints = readMeSummary().get("weeklyFootprints");
        return footprints.get(footprints.size() - 1).get("completionCount").asInt();
    }

    private int monthlyCompletionCount() throws Exception {
        String body = mockMvc.perform(get("/api/v1/home")).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("monthlyCompletionCount").asInt();
    }

    private JsonNode readMeSummary() throws Exception {
        String body = mockMvc.perform(get("/api/v1/me/summary")).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private String statusCodesOf(String homeBody) {
        JsonNode categories = objectMapper.readTree(homeBody).get("data").get("categories");
        StringBuilder codes = new StringBuilder();
        for (JsonNode category : categories) {
            codes.append(category.get("status").get("code").asText()).append(',');
        }
        return codes.toString();
    }
}
