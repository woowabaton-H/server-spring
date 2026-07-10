package cleanloop.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * 댓글/답변 조회·작성과 글 작성 검증.
 * 시드: 클라이언트 프로토타입 커뮤니티 글과 문서 기반 Q&A/팁을 함께 적재한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityCommentApiTest {

    private static final String TIP1 = "e0000000-0000-0000-0000-000000000001";
    private static final String QA1 = "e0000000-0000-0000-0000-000000000005";
    private static final String MISSING = "e0000000-0000-0000-0000-0000000000ff";
    private static final int SEEDED_QA_COUNT = 19;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------- 댓글 조회 ----------

    @Test
    void 댓글을_오래된_순으로_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1 + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data[0].body").value("물때 제거제를 매일 쓰는 것보다 물기 제거를 먼저 해보세요."))
                .andExpect(jsonPath("$.data[1].body").value("거울 얼룩은 마른 극세사 천으로 마무리하면 덜 남습니다."))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist());
    }

    @Test
    void 댓글에는_작성자_이름과_본인_여부가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1 + "/comments"))
                .andExpect(jsonPath("$.data[0].postId").value(TIP1))
                .andExpect(jsonPath("$.data[0].authorName").value("보송"))
                // 데모 사용자가 유일한 사용자라 시드 댓글도 본인 작성이다
                .andExpect(jsonPath("$.data[0].authorIsMe").value(true))
                .andExpect(jsonPath("$.data[0].createdAt").exists());
    }

    @Test
    void 댓글이_없는_글은_빈_목록이다() throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "댓글 없는 글", "tag": "욕실", "body": "본문입니다."}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(body).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/community/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(0)));
    }

    @Test
    void 없는_글의_댓글을_조회하면_404다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + MISSING + "/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_POST_NOT_FOUND"));
    }

    @Test
    void 댓글_커서로_이어_읽으면_중복과_누락이_없다() throws Exception {
        List<String> collected = new ArrayList<>();
        String cursor = null;

        do {
            var request = get("/api/v1/community/posts/" + QA1 + "/comments").param("limit", "1");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            String body = mockMvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            for (JsonNode comment : objectMapper.readTree(body).get("data")) {
                collected.add(comment.get("id").asText());
            }
            JsonNode next = objectMapper.readTree(body).path("meta").path("nextCursor");
            cursor = next.isNull() || next.isMissingNode() ? null : next.asText();
        } while (cursor != null);

        assertThat(collected).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void 잘못된_댓글_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1 + "/comments").param("cursor", "!!nope!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURSOR"));
    }

    // ---------- 댓글 작성 ----------

    @Test
    void tips_글에_댓글을_남기면_commentsCount가_증가한다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + TIP1 + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "저도 이 방법 써봤는데 효과 있었어요."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(TIP1))
                .andExpect(jsonPath("$.data.body").value("저도 이 방법 써봤는데 효과 있었어요."))
                .andExpect(jsonPath("$.data.authorIsMe").value(true));

        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.commentsCount").value(3))
                // tips 글이므로 답변 수는 그대로다
                .andExpect(jsonPath("$.data.answersCount").value(0));
    }

    @Test
    void qa_글에_답변을_남기면_answersCount가_증가한다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + QA1 + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "중성세제로 먼저 테스트해보세요."}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/community/posts/" + QA1))
                .andExpect(jsonPath("$.data.answersCount").value(3))
                // qa 글이므로 댓글 수는 그대로다
                .andExpect(jsonPath("$.data.commentsCount").value(0));
    }

    @Test
    void 작성한_댓글이_목록에_이어_붙는다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + TIP1 + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "가장 최근 댓글"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/community/posts/" + TIP1 + "/comments"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                // 오래된 순이므로 새 댓글이 마지막이다
                .andExpect(jsonPath("$.data[2].body").value("가장 최근 댓글"));
    }

    @Test
    void 시드_댓글_수는_집계_컬럼과_일치한다() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.commentsCount").value(2));
        mockMvc.perform(get("/api/v1/community/posts/" + TIP1 + "/comments"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)));

        mockMvc.perform(get("/api/v1/community/posts/" + QA1))
                .andExpect(jsonPath("$.data.answersCount").value(2));
        mockMvc.perform(get("/api/v1/community/posts/" + QA1 + "/comments"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)));
    }

    @Test
    void 빈_댓글은_422다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + TIP1 + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "   "}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 없는_글에_댓글을_남기면_404다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + MISSING + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "안녕하세요"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_POST_NOT_FOUND"));
    }

    @Test
    void 댓글_작성에_실패하면_카운터가_오르지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts/" + TIP1 + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": ""}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/community/posts/" + TIP1))
                .andExpect(jsonPath("$.data.commentsCount").value(2));
    }

    // ---------- 글 작성 ----------

    @Test
    void tips_글을_작성한다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "욕실 물때는 물기 제거가 먼저입니다",
                                 "tag": "욕실", "body": "샤워 후 벽면과 거울의 물기를 바로 제거하면 물때가 줄어듭니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.type").value("tips"))
                .andExpect(jsonPath("$.data.title").value("욕실 물때는 물기 제거가 먼저입니다"))
                .andExpect(jsonPath("$.data.tag").value("욕실"))
                .andExpect(jsonPath("$.data.body").exists())
                // 새 글의 집계는 모두 0에서 시작한다
                .andExpect(jsonPath("$.data.helpfulCount").value(0))
                .andExpect(jsonPath("$.data.commentsCount").value(0))
                .andExpect(jsonPath("$.data.answersCount").value(0))
                .andExpect(jsonPath("$.data.savedCount").value(0))
                .andExpect(jsonPath("$.data.isSaved").value(false))
                .andExpect(jsonPath("$.data.hasMarkedHelpful").value(false));
    }

    @Test
    void 작성한_글이_목록에_나타난다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "qa", "title": "새 질문입니다", "tag": "주방", "body": "본문입니다."}
                                """))
                .andExpect(status().isCreated());

        // 인기 점수 0이라 목록 마지막에 붙는다
        mockMvc.perform(get("/api/v1/community/posts").param("type", "qa"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(SEEDED_QA_COUNT + 1)))
                .andExpect(jsonPath("$.data[?(@.title == '새 질문입니다')].isPopular")
                        .value(Matchers.hasItem(false)));
    }

    @Test
    void 작성한_글을_상세로_다시_읽을_수_있다() throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "제목", "tag": "바닥", "body": "본문"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(body).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/community/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    void 작성한_글에_바로_댓글을_남길_수_있다() throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "제목", "tag": "바닥", "body": "본문"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(body).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "첫 댓글"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/community/posts/" + postId))
                .andExpect(jsonPath("$.data.commentsCount").value(1));
    }

    @Test
    void tag_없이도_글을_작성한다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "태그 없는 글", "body": "본문"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tag").doesNotExist());
    }

    @Test
    void 허용되지_않는_type으로_글을_쓰면_400이다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "notice", "title": "제목", "body": "본문"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.type").value("notice"));
    }

    @Test
    void 제목이나_본문이_비면_422다() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "  ", "body": "본문"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.details.title").exists());

        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "tips", "title": "제목", "body": ""}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.details.body").exists());
    }

    @Test
    void 제목이_160자를_넘으면_422다() throws Exception {
        String tooLong = "가".repeat(161);

        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"tips\",\"title\":\"%s\",\"body\":\"본문\"}".formatted(tooLong)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.details.title").exists());
    }
}
