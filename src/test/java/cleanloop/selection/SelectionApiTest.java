package cleanloop.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * docs/issues/05-selection.md 완료 기준 검증.
 * 시드: 공개 셀렉션 7종, trash-pickup에 제공업체 3곳, starter-kit은 저장된 상태.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelectionApiTest {

    private static final String PROVIDER_ID = "d0000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 공개된_셀렉션을_모두_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/selections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(7)))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist());
    }

    @Test
    void 목록의_식별자는_slug다() throws Exception {
        mockMvc.perform(get("/api/v1/selections"))
                .andExpect(jsonPath("$.data[0].id").value("starter-kit"))
                .andExpect(jsonPath("$.data[0].type").value("kit"))
                .andExpect(jsonPath("$.data[0].typeLabel").value("키트"));
    }

    @Test
    void 강조_항목이_먼저_오고_그_안에서는_sort_order_순이다() throws Exception {
        JsonNode selections = readData(mockMvc.perform(get("/api/v1/selections"))
                .andReturn().getResponse().getContentAsString());

        List<String> ids = new ArrayList<>();
        boolean seenNotHighlighted = false;
        for (JsonNode selection : selections) {
            ids.add(selection.get("id").asText());
            boolean highlighted = selection.get("isHighlighted").asBoolean();
            if (!highlighted) {
                seenNotHighlighted = true;
            } else {
                // 강조 항목이 비강조 항목 뒤에 나오면 안 된다
                assertThat(seenNotHighlighted).isFalse();
            }
        }
        assertThat(ids.subList(0, 3))
                .containsExactly("starter-kit", "bath-soft-start", "trash-pickup");
    }

    @Test
    void 특정_카테고리를_요청하면_전체_항목도_함께_반환한다() throws Exception {
        JsonNode selections = readData(mockMvc.perform(get("/api/v1/selections").param("category", "욕실"))
                .andReturn().getResponse().getContentAsString());

        List<String> categories = new ArrayList<>();
        for (JsonNode selection : selections) {
            categories.add(selection.get("category").asText());
        }
        assertThat(categories).containsOnly("욕실", "전체");
        assertThat(categories).contains("욕실", "전체");
    }

    @Test
    void 카테고리_전체를_요청하면_모든_항목을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/selections").param("category", "전체"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(7)));
    }

    @Test
    void type으로_필터한다() throws Exception {
        mockMvc.perform(get("/api/v1/selections").param("type", "service"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data[*].type").value(Matchers.everyItem(Matchers.is("service"))));
    }

    @Test
    void 목록에_카드_표시용_필드가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/selections"))
                .andExpect(jsonPath("$.data[0].id").value("starter-kit"))
                .andExpect(jsonPath("$.data[0].imageUrl")
                        .value("https://cdn.cleanloop.example/selections/starter-kit.jpg"))
                .andExpect(jsonPath("$.data[0].ratingText").value("4.8"))
                .andExpect(jsonPath("$.data[0].reviewCountText").value("후기 1,240개"))
                .andExpect(jsonPath("$.data[0].tags").value(Matchers.contains("입문용", "가성비", "한 번에 준비")));
    }

    /** 카드에 필요한 정보라 목록에도 담지만, 판단 근거인 notice와 checks는 상세 전용이다. */
    @Test
    void 목록에는_notice와_checks가_없다() throws Exception {
        mockMvc.perform(get("/api/v1/selections"))
                .andExpect(jsonPath("$.data[0].notice").doesNotExist())
                .andExpect(jsonPath("$.data[0].checks").doesNotExist())
                .andExpect(jsonPath("$.data[0].tags").exists());
    }

    @Test
    void 상세에_확인_항목이_순서대로_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/selections/bath-soft-start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ratingText").value("4.6"))
                .andExpect(jsonPath("$.data.reviewCountText").value("후기 832개"))
                .andExpect(jsonPath("$.data.tags").value(Matchers.contains("욕실", "물때")))
                .andExpect(jsonPath("$.data.checks").value(Matchers.contains(
                        "대리석 등 약한 소재에는 산성 세제를 쓰지 마세요.",
                        "사용 중 환기를 유지하세요.")))
                .andExpect(jsonPath("$.data.notice").exists());
    }

    @Test
    void 태그가_sort_order_순으로_반환된다() throws Exception {
        mockMvc.perform(get("/api/v1/selections/trash-pickup"))
                .andExpect(jsonPath("$.data.tags").value(Matchers.contains("정기 수거", "구독", "문앞 배출")));
    }

    @Test
    void 저장한_셀렉션에도_카드_표시용_필드가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/me/saved-selections"))
                .andExpect(jsonPath("$.data[0].id").value("starter-kit"))
                .andExpect(jsonPath("$.data[0].imageUrl").exists())
                .andExpect(jsonPath("$.data[0].ratingText").value("4.8"))
                .andExpect(jsonPath("$.data[0].tags", Matchers.hasSize(3)));
    }

    @Test
    void 마이_요약의_저장_셀렉션에도_카드_표시용_필드가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(jsonPath("$.data.savedSelections[0].id").value("starter-kit"))
                .andExpect(jsonPath("$.data.savedSelections[0].imageUrl").exists())
                .andExpect(jsonPath("$.data.savedSelections[0].tags", Matchers.hasSize(3)));
    }

    @Test
    void 목록에는_저장_여부가_담기고_제공업체는_비어_있다() throws Exception {
        mockMvc.perform(get("/api/v1/selections"))
                // 시드에서 starter-kit만 저장된 상태다
                .andExpect(jsonPath("$.data[0].id").value("starter-kit"))
                .andExpect(jsonPath("$.data[0].isSaved").value(true))
                .andExpect(jsonPath("$.data[0].providers", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data[1].isSaved").value(false));
    }

    @Test
    void 상세는_제공업체와_고지_문구를_함께_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/selections/trash-pickup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("trash-pickup"))
                .andExpect(jsonPath("$.data.typeLabel").value("구독 연결"))
                .andExpect(jsonPath("$.data.notice").exists())
                .andExpect(jsonPath("$.data.providers", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data.providers[0].name").value("오늘수거"))
                .andExpect(jsonPath("$.data.providers[0].ratingText").value("4.8"))
                .andExpect(jsonPath("$.data.providers[2].name").value("클린박스"));
    }

    @Test
    void 없는_셀렉션은_404다() throws Exception {
        mockMvc.perform(get("/api/v1/selections/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SELECTION_NOT_FOUND"));
    }

    @Test
    void 셀렉션을_저장한다() throws Exception {
        mockMvc.perform(put("/api/v1/selections/bath-soft-start/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionId").value("bath-soft-start"))
                .andExpect(jsonPath("$.data.isSaved").value(true))
                .andExpect(jsonPath("$.data.savedAt").exists());

        mockMvc.perform(get("/api/v1/selections/bath-soft-start"))
                .andExpect(jsonPath("$.data.isSaved").value(true));
    }

    @Test
    void 같은_항목을_반복_저장해도_결과가_같다() throws Exception {
        String first = mockMvc.perform(put("/api/v1/selections/bath-soft-start/save"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(put("/api/v1/selections/bath-soft-start/save"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(savedAtOf(second)).isEqualTo(savedAtOf(first));
        mockMvc.perform(get("/api/v1/me/saved-selections"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)));
    }

    @Test
    void 저장을_해제한다() throws Exception {
        mockMvc.perform(delete("/api/v1/selections/starter-kit/save"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/selections/starter-kit"))
                .andExpect(jsonPath("$.data.isSaved").value(false));
        mockMvc.perform(get("/api/v1/me/saved-selections"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(0)));
    }

    @Test
    void 저장되지_않은_항목을_해제해도_204다() throws Exception {
        mockMvc.perform(delete("/api/v1/selections/floor-easy/save"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/selections/floor-easy/save"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 없는_셀렉션_저장은_404다() throws Exception {
        mockMvc.perform(put("/api/v1/selections/nope/save"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SELECTION_NOT_FOUND"));
    }

    @Test
    void 저장한_셀렉션을_저장_시각_최신순으로_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/selections/floor-easy/save")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/saved-selections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                // 방금 저장한 항목이 먼저 온다
                .andExpect(jsonPath("$.data[0].id").value("floor-easy"))
                .andExpect(jsonPath("$.data[0].isSaved").value(true))
                .andExpect(jsonPath("$.data[1].id").value("starter-kit"));
    }

    @Test
    void 저장_상태가_목록_상세_마이에_일관되게_반영된다() throws Exception {
        mockMvc.perform(put("/api/v1/selections/sink-weekly/save")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/selections/sink-weekly"))
                .andExpect(jsonPath("$.data.isSaved").value(true));
        mockMvc.perform(get("/api/v1/selections").param("type", "product"))
                .andExpect(jsonPath("$.data[?(@.id == 'sink-weekly')].isSaved")
                        .value(Matchers.hasItem(true)));
        mockMvc.perform(get("/api/v1/me/summary"))
                .andExpect(jsonPath("$.data.stats.savedSelectionCount").value(2))
                .andExpect(jsonPath("$.data.savedSelections[0].id").value("sink-weekly"));
    }

    @Test
    void 외부_보기_클릭을_기록하고_이동_정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/selections/trash-pickup/external-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "%s"}
                                """.formatted(PROVIDER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionId").value("trash-pickup"))
                .andExpect(jsonPath("$.data.providerId").value(PROVIDER_ID))
                // 시드에는 외부 URL이 없다. 고지 문구는 그래도 함께 내려준다
                .andExpect(jsonPath("$.data.externalUrl").doesNotExist())
                .andExpect(jsonPath("$.data.notice").exists());
    }

    @Test
    void providerId_없이도_외부_보기를_기록한다() throws Exception {
        mockMvc.perform(post("/api/v1/selections/starter-kit/external-view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionId").value("starter-kit"))
                .andExpect(jsonPath("$.data.notice").exists());
    }

    @Test
    void 다른_셀렉션의_제공업체를_지정하면_404다() throws Exception {
        mockMvc.perform(post("/api/v1/selections/starter-kit/external-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "%s"}
                                """.formatted(PROVIDER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROVIDER_NOT_FOUND"));
    }

    @Test
    void 커서로_전체_목록을_이어_읽으면_중복과_누락이_없다() throws Exception {
        List<String> collected = new ArrayList<>();
        String cursor = null;

        do {
            var request = get("/api/v1/selections").param("limit", "2");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            String body = mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (JsonNode selection : readData(body)) {
                collected.add(selection.get("id").asText());
            }
            JsonNode next = objectMapper.readTree(body).path("meta").path("nextCursor");
            cursor = next.isNull() || next.isMissingNode() ? null : next.asText();
        } while (cursor != null);

        assertThat(collected).hasSize(7).doesNotHaveDuplicates();
        assertThat(collected.get(0)).isEqualTo("starter-kit");
    }

    @Test
    void 잘못된_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/selections").param("cursor", "!!nope!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURSOR"));
    }

    private JsonNode readData(String body) {
        return objectMapper.readTree(body).get("data");
    }

    private String savedAtOf(String body) {
        return objectMapper.readTree(body).get("data").get("savedAt").asText();
    }
}
