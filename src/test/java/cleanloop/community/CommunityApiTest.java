package cleanloop.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * docs/issues/06-community.md 완료 기준 검증.
 *
 * 시드 인기 점수(helpful + saved):
 *   tips: 이미지 문서글 340, 339, 338, 337, ... 333 -> 상위 3 문턱값 338
 *   qa:   이미지 문서글 240, 239, 238, 237, qa1 110 -> 상위 3 문턱값 238
 *   이미지가 있는 문서 기반 tips/qa가 각 목록 최상단에 오도록 높은 점수로 추가된다.
 * 시드 반응: tip1에 도움됨, qa1에 저장.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityApiTest {

    private static final String DOC_TIP1 = "e1000000-0000-0000-0000-000000000001";
    private static final String DOC_TIP2 = "e1000000-0000-0000-0000-000000000002";
    private static final String DOC_TIP3 = "e1000000-0000-0000-0000-000000000003";
    private static final String DOC_TIP4 = "e1000000-0000-0000-0000-000000000004";
    private static final String DOC_TIP5 = "e1000000-0000-0000-0000-000000000005";
    private static final String DOC_TIP6 = "e1000000-0000-0000-0000-000000000006";
    private static final String DOC_TIP7 = "e1000000-0000-0000-0000-000000000007";
    private static final String DOC_TIP8 = "e1000000-0000-0000-0000-000000000008";
    private static final String DOC_QA_IMAGE1 = "e1000000-0000-0000-0000-000000000101";
    private static final String TIP1 = "e0000000-0000-0000-0000-000000000001";
    private static final String TIP2 = "e0000000-0000-0000-0000-000000000002";
    private static final String TIP4 = "e0000000-0000-0000-0000-000000000004";
    private static final String QA1 = "e0000000-0000-0000-0000-000000000005";
    private static final String MISSING = "e0000000-0000-0000-0000-0000000000ff";
    private static final int TIPS_COUNT = 12;
    private static final int QA_COUNT = 19;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void type으로_글을_필터한다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(TIPS_COUNT)))
                .andExpect(jsonPath("$.data[*].type").value(Matchers.everyItem(Matchers.is("tips"))));

        mockMvc.perform(get("/api/v1/community/posts").param("type", "qa"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(QA_COUNT)));
    }

    @Test
    void type은_필수다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 허용되지_않는_type은_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.type").value("nope"));
    }

    @Test
    void tag로_글을_좁힌다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips").param("tag", "욕실"))
                .andExpect(jsonPath("$.data[*].id").value(Matchers.hasItem(TIP1)))
                .andExpect(jsonPath("$.data[*].tag").value(Matchers.everyItem(Matchers.is("욕실"))));
    }

    /**
     * 인기 여부는 글 자체의 속성이다. 태그로 좁혔다고 1위 글이 비인기로 뒤집히면 안 된다.
     */
    @Test
    void isPopular은_tag_필터에_영향받지_않는다() throws Exception {
        // 욕실 문서글은 tips 전체 상위 3건 안에 있으므로 태그로 좁혀도 인기 글이다
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips").param("tag", "욕실"))
                .andExpect(jsonPath("$.data[0].id").value(DOC_TIP2))
                .andExpect(jsonPath("$.data[0].isPopular").value(true));

        // 바닥 문서글은 상위 3건 밖이므로 태그로 좁혀도 비인기다
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips").param("tag", "바닥"))
                .andExpect(jsonPath("$.data[0].id").value(DOC_TIP5))
                .andExpect(jsonPath("$.data[0].isPopular").value(false));
    }

    @Test
    void 인기순으로_정렬한다() throws Exception {
        JsonNode posts = readData(mockMvc.perform(get("/api/v1/community/posts").param("type", "tips"))
                .andReturn().getResponse().getContentAsString());

        List<Integer> scores = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        for (JsonNode post : posts) {
            scores.add(post.get("helpfulCount").asInt() + post.get("savedCount").asInt());
            ids.add(post.get("id").asText());
            imageUrls.add(post.get("imageUrl").asText());
        }
        assertThat(scores.subList(0, 4)).containsExactly(340, 339, 338, 337);
        assertThat(ids.subList(0, 8)).containsExactly(DOC_TIP1, DOC_TIP2, DOC_TIP3, DOC_TIP4,
                DOC_TIP5, DOC_TIP6, DOC_TIP7, DOC_TIP8);
        assertThat(imageUrls.subList(0, 8)).allSatisfy(imageUrl -> assertThat(imageUrl).isNotBlank());
    }

    @Test
    void 상위_세건만_isPopular다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips"))
                .andExpect(jsonPath("$.data[0].isPopular").value(true))
                .andExpect(jsonPath("$.data[1].isPopular").value(true))
                .andExpect(jsonPath("$.data[2].isPopular").value(true))
                .andExpect(jsonPath("$.data[3].isPopular").value(false));
    }

    @Test
    void 목록에는_본문_미리보기와_저장_여부가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips"))
                .andExpect(jsonPath("$.data[0].bodyPreview").exists())
                .andExpect(jsonPath("$.data[0].isSaved").value(false));

        // Q&A 첫 글은 이미지 문서글이고, 시드에서 qa1만 저장된 상태다
        mockMvc.perform(get("/api/v1/community/posts").param("type", "qa"))
                .andExpect(jsonPath("$.data[0].id").value(DOC_QA_IMAGE1))
                .andExpect(jsonPath("$.data[?(@.id == '%s')].isSaved".formatted(QA1))
                        .value(Matchers.hasItem(true)));
    }

    @Test
    void 커뮤니티_목록과_상세에_이미지_url이_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "qa").param("limit", "100"))
                .andExpect(jsonPath("$.data[0].id").value(DOC_QA_IMAGE1))
                .andExpect(jsonPath("$.data[?(@.id == '%s')].imageUrl".formatted(DOC_QA_IMAGE1))
                        .value(Matchers.hasItem("/cleanloop/images/qna-washer-gasket-mold.png")));

        mockMvc.perform(get("/api/v1/community/posts/" + DOC_QA_IMAGE1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value("/cleanloop/images/qna-washer-gasket-mold.png"));
    }

    @Test
    void 상세는_본문_전문과_사용자별_상태를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TIP1))
                .andExpect(jsonPath("$.data.body").exists())
                .andExpect(jsonPath("$.data.helpfulCount").value(128))
                // 시드에서 tip1에 도움됨을 표시해뒀다
                .andExpect(jsonPath("$.data.hasMarkedHelpful").value(true))
                .andExpect(jsonPath("$.data.isSaved").value(false));
    }

    @Test
    void 없는_글은_404다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + MISSING))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_POST_NOT_FOUND"));
    }

    @Test
    void 도움됨을_표시하면_카운터가_1_증가한다() throws Exception {
        mockMvc.perform(put("/api/v1/community/posts/" + TIP2 + "/helpful"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(TIP2))
                .andExpect(jsonPath("$.data.hasMarkedHelpful").value(true))
                .andExpect(jsonPath("$.data.helpfulCount").value(105));
    }

    @Test
    void 같은_사용자의_중복_도움됨은_1회로_제한된다() throws Exception {
        // tip1은 시드에서 이미 도움됨 상태다. 다시 눌러도 128 그대로여야 한다
        mockMvc.perform(put("/api/v1/community/posts/" + TIP1 + "/helpful"))
                .andExpect(jsonPath("$.data.helpfulCount").value(128));
        mockMvc.perform(put("/api/v1/community/posts/" + TIP1 + "/helpful"))
                .andExpect(jsonPath("$.data.helpfulCount").value(128));

        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.helpfulCount").value(128));
    }

    @Test
    void 도움됨을_취소하면_카운터가_1_감소한다() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/" + TIP1 + "/helpful"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMarkedHelpful").value(false))
                .andExpect(jsonPath("$.data.helpfulCount").value(127));

        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.hasMarkedHelpful").value(false))
                .andExpect(jsonPath("$.data.helpfulCount").value(127));
    }

    @Test
    void 표시하지_않은_도움됨을_취소해도_카운터가_줄지_않는다() throws Exception {
        // tip2에는 반응이 없다
        mockMvc.perform(delete("/api/v1/community/posts/" + TIP2 + "/helpful"))
                .andExpect(jsonPath("$.data.helpfulCount").value(104));
        mockMvc.perform(delete("/api/v1/community/posts/" + TIP2 + "/helpful"))
                .andExpect(jsonPath("$.data.helpfulCount").value(104));
    }

    @Test
    void 도움됨_취소를_반복해도_카운터가_음수가_되지_않는다() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(delete("/api/v1/community/posts/" + TIP1 + "/helpful"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.helpfulCount").value(127));
    }

    @Test
    void 표시와_취소를_반복해도_카운터가_어긋나지_않는다() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(put("/api/v1/community/posts/" + TIP2 + "/helpful"))
                    .andExpect(jsonPath("$.data.helpfulCount").value(105));
            mockMvc.perform(delete("/api/v1/community/posts/" + TIP2 + "/helpful"))
                    .andExpect(jsonPath("$.data.helpfulCount").value(104));
        }
    }

    @Test
    void 없는_글에_도움됨을_표시하면_404다() throws Exception {
        mockMvc.perform(put("/api/v1/community/posts/" + MISSING + "/helpful"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_POST_NOT_FOUND"));
    }

    @Test
    void 글을_저장하면_저장_수가_증가한다() throws Exception {
        mockMvc.perform(put("/api/v1/community/posts/" + TIP4 + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(TIP4))
                .andExpect(jsonPath("$.data.isSaved").value(true))
                .andExpect(jsonPath("$.data.savedAt").exists());

        mockMvc.perform(get("/api/v1/community/posts/" + TIP4))
                .andExpect(jsonPath("$.data.isSaved").value(true))
                .andExpect(jsonPath("$.data.savedCount").value(39));
    }

    @Test
    void 같은_글을_반복_저장해도_결과가_같다() throws Exception {
        String first = mockMvc.perform(put("/api/v1/community/posts/" + TIP4 + "/save"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(put("/api/v1/community/posts/" + TIP4 + "/save"))
                .andReturn().getResponse().getContentAsString();

        assertThat(savedAtOf(second)).isEqualTo(savedAtOf(first));
        mockMvc.perform(get("/api/v1/community/posts/" + TIP4))
                .andExpect(jsonPath("$.data.savedCount").value(39));
    }

    @Test
    void 저장을_해제하면_저장_수가_감소한다() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/" + QA1 + "/save"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/community/posts/" + QA1))
                .andExpect(jsonPath("$.data.isSaved").value(false))
                .andExpect(jsonPath("$.data.savedCount").value(21));
    }

    @Test
    void 저장하지_않은_글을_해제해도_204이고_저장_수가_줄지_않는다() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/" + TIP4 + "/save"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/community/posts/" + TIP4))
                .andExpect(jsonPath("$.data.savedCount").value(38));
    }

    /** 저장 수가 바뀌면 인기 점수도 함께 움직인다. */
    @Test
    void 저장이_인기_점수에_반영된다() throws Exception {
        // 기존 tip4를 저장해도 이미지 문서글 상위권을 넘지 못함을 확인한다
        mockMvc.perform(put("/api/v1/community/posts/" + TIP4 + "/save")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips"))
                .andExpect(jsonPath("$.data[?(@.id == '%s')].savedCount".formatted(TIP4))
                        .value(Matchers.hasItem(39)))
                .andExpect(jsonPath("$.data[?(@.id == '%s')].isPopular".formatted(TIP4))
                        .value(Matchers.hasItem(false)));
    }

    @Test
    void 커서로_이어_읽으면_중복과_누락이_없다() throws Exception {
        List<String> collected = new ArrayList<>();
        String cursor = null;

        do {
            var request = get("/api/v1/community/posts").param("type", "tips").param("limit", "2");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            String body = mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (JsonNode post : readData(body)) {
                collected.add(post.get("id").asText());
            }
            JsonNode next = objectMapper.readTree(body).path("meta").path("nextCursor");
            cursor = next.isNull() || next.isMissingNode() ? null : next.asText();
        } while (cursor != null);

        assertThat(collected).hasSize(TIPS_COUNT).doesNotHaveDuplicates();
        assertThat(collected.get(0)).isEqualTo(DOC_TIP1);
    }

    @Test
    void isPopular은_페이지를_넘겨도_같은_판정이다() throws Exception {
        String secondPageCursor = objectMapper.readTree(
                        mockMvc.perform(get("/api/v1/community/posts")
                                        .param("type", "tips").param("limit", "3"))
                                .andReturn().getResponse().getContentAsString())
                .path("meta").path("nextCursor").asText();

        // 2페이지의 첫 글은 상위 3건 밖이므로 인기 글이 아니다
        mockMvc.perform(get("/api/v1/community/posts")
                        .param("type", "tips").param("limit", "3").param("cursor", secondPageCursor))
                .andExpect(jsonPath("$.data[0].id").value(DOC_TIP4))
                .andExpect(jsonPath("$.data[0].isPopular").value(false));
    }

    @Test
    void 잘못된_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts").param("type", "tips").param("cursor", "!!nope!!"))
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
