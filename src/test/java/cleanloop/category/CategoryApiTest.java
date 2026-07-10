package cleanloop.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * docs/issues/02-category.md 완료 기준 검증.
 * 시드 데이터의 욕실 카테고리 id는 b0000000-...-01 이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryApiTest {

    private static final String BATH_ID = "b0000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 활성_카테고리를_sort_order_순으로_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(6)))
                .andExpect(jsonPath("$.data[0].name").value("욕실"))
                .andExpect(jsonPath("$.data[1].name").value("주방"))
                .andExpect(jsonPath("$.data[5].name").value("계절/가전"));
    }

    @Test
    void 각_카테고리에_계산된_상태와_nextDueAt이_포함된다() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data[0].nextDueAt").exists())
                .andExpect(jsonPath("$.data[0].status.code").exists())
                .andExpect(jsonPath("$.data[0].status.label").exists())
                .andExpect(jsonPath("$.data[0].status.daysUntilNext").exists());
    }

    /**
     * 시드가 "오늘 기준 N일 전"으로 심겨 있어 실행 시점과 무관하게 상태가 재현된다.
     */
    @Test
    void 시드_데이터의_상태가_의도대로_계산된다() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data[0].status.code").value("due"))    // 욕실  cycle 14, -16일
                .andExpect(jsonPath("$.data[1].status.code").value("soon"))   // 주방  cycle 7,  -6일
                .andExpect(jsonPath("$.data[2].status.code").value("good"))   // 세탁  cycle 14, -3일
                .andExpect(jsonPath("$.data[3].status.code").value("soon"))   // 쓰레기 cycle 3, -2일
                .andExpect(jsonPath("$.data[4].status.code").value("good"))   // 바닥  cycle 7,  -1일
                .andExpect(jsonPath("$.data[5].status.code").value("good"));  // 계절  cycle 28, -25일
    }

    @Test
    void 프리셋_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/category-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(7)))
                .andExpect(jsonPath("$.data[0].key").value("bath"))
                .andExpect(jsonPath("$.data[6].key").value("pet"))
                .andExpect(jsonPath("$.data[6].cycleDays").value(7));
    }

    @Test
    void 프리셋으로_카테고리를_생성한다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"presetKey": "pet"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("반려동물"))
                .andExpect(jsonPath("$.data.icon").value("floor"))
                .andExpect(jsonPath("$.data.cycleDays").value(7))
                // 생성 직후에는 방금 완료한 셈이므로 주기만큼 여유가 있다
                .andExpect(jsonPath("$.data.status.code").value("good"))
                .andExpect(jsonPath("$.data.status.daysUntilNext").value(7));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(7)));
    }

    @Test
    void 직접_입력으로_카테고리를_생성한다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "현관", "icon": "floor", "cycleDays": 7,
                                 "note": "신발장과 현관 먼지를 함께 관리해요."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("현관"))
                .andExpect(jsonPath("$.data.note").value("신발장과 현관 먼지를 함께 관리해요."));
    }

    @Test
    void 같은_프리셋의_활성_카테고리가_있으면_409다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"presetKey": "bath"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_DUPLICATED"));
    }

    @Test
    void 같은_이름의_활성_카테고리가_있으면_409다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "욕실", "icon": "bath", "cycleDays": 7}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_DUPLICATED"));
    }

    @Test
    void 존재하지_않는_프리셋이면_404다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"presetKey": "nope"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_PRESET_NOT_FOUND"));
    }

    @Test
    void 직접_입력에_필수값이_빠지면_422다() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "현관"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 주기를_수정하면_상태가_재계산된다() throws Exception {
        // 욕실은 16일 전 완료. 주기를 28일로 늘리면 12일 남아 good이 된다
        mockMvc.perform(patch("/api/v1/categories/" + BATH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleDays": 28}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycleDays").value(28))
                .andExpect(jsonPath("$.data.status.code").value("good"))
                .andExpect(jsonPath("$.data.status.daysUntilNext").value(12));
    }

    @Test
    void 이름과_설명을_수정한다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/" + BATH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "화장실", "note": "물때를 기준으로 관리해요."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("화장실"))
                .andExpect(jsonPath("$.data.note").value("물때를 기준으로 관리해요."))
                // 생략한 주기는 그대로 유지된다
                .andExpect(jsonPath("$.data.cycleDays").value(14));
    }

    @Test
    void 허용되지_않는_주기는_422다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/" + BATH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleDays": 10}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.cycleDays").value(10));
    }

    @Test
    void 다른_카테고리와_이름이_겹치면_409다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/" + BATH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "주방"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_DUPLICATED"));
    }

    @Test
    void 자기_이름으로의_수정은_중복이_아니다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/" + BATH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "욕실", "cycleDays": 21}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycleDays").value(21));
    }

    @Test
    void 없는_카테고리를_수정하면_404다() throws Exception {
        mockMvc.perform(patch("/api/v1/categories/b0000000-0000-0000-0000-0000000000ff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cycleDays": 7}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }
}
