package cleanloop.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cleanloop.common.user.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시드: 욕실(유일한 due 카테고리) 미확인 알림 1건 + 주방 읽음 알림 1건.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationApiTest {

    private static final UUID UNREAD = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID READ = UUID.fromString("11000000-0000-0000-0000-000000000002");
    private static final String MISSING = "11000000-0000-0000-0000-0000000000ff";

    private static final String BATH_CATEGORY = "b0000000-0000-0000-0000-000000000001";
    private static final String FLOOR_CATEGORY = "b0000000-0000-0000-0000-000000000005";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    // ---------- 목록 ----------

    @Test
    void 알림_목록은_미확인_우선_최신순이다() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications", Matchers.hasSize(2)))
                // 주방 알림이 더 오래됐지만 읽은 알림이라 뒤로 간다
                .andExpect(jsonPath("$.data.notifications[0].id").value(UNREAD.toString()))
                .andExpect(jsonPath("$.data.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.data.notifications[1].id").value(READ.toString()))
                .andExpect(jsonPath("$.data.notifications[1].isRead").value(true));
    }

    @Test
    void 미확인_수는_meta가_아니라_data에_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.meta.unreadCount").doesNotExist());
    }

    @Test
    void 알림에는_카테고리_이름과_이동_경로가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.notifications[0].categoryId").value(BATH_CATEGORY))
                .andExpect(jsonPath("$.data.notifications[0].categoryName").value("욕실"))
                .andExpect(jsonPath("$.data.notifications[0].deepLink").value("/categories/" + BATH_CATEGORY))
                .andExpect(jsonPath("$.data.notifications[0].createdAt").exists());
    }

    /** final-plan 12.3: 못 한 일을 지적하지 않고 챙기면 좋은 일을 알려주는 톤이어야 한다. */
    @Test
    void 알림_문구는_지적형이_아니라_제안형이다() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.notifications[0].title").value("이번 주에는 욕실만 챙겨도 충분해요"))
                .andExpect(jsonPath("$.data.notifications[0].body")
                        .value(Matchers.containsString("완료하면 다음 관리는 자동으로 다시 잡아둘게요")));
    }

    // ---------- 개별 읽음 ----------

    @Test
    void 알림을_읽으면_미확인_수가_줄어든다() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/" + UNREAD + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(UNREAD.toString()))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").exists());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void 이미_읽은_알림을_다시_읽어도_처음_읽은_시각은_그대로다() throws Exception {
        LocalDateTime before = readAtOf(READ);

        mockMvc.perform(post("/api/v1/notifications/" + READ + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        assertThat(readAtOf(READ)).isEqualTo(before);
    }

    @Test
    void 없는_알림을_읽으면_404다() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/" + MISSING + "/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void 알림_id가_uuid가_아니면_400이며_내부_예외가_새지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/not-a-uuid/read"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message")
                        .value(Matchers.not(Matchers.containsString("java.util.UUID"))));
    }

    // ---------- 전체 읽음 ----------

    @Test
    void 전체_읽음_처리하면_미확인_수가_0이_된다() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(1))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void 전체_읽음_처리를_반복해도_updatedCount만_0이_된다() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all")).andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(0))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ---------- 홈 연동 ----------

    @Test
    void 홈이_미확인_알림_수를_돌려준다() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(1));

        mockMvc.perform(put("/api/v1/notifications/read-all")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/home"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(0));
    }

    // ---------- 완료 연동 (backend-design 13.1-5) ----------

    @Test
    void 카테고리를_완료하면_그_카테고리의_미확인_알림이_읽음_처리된다() throws Exception {
        mockMvc.perform(post("/api/v1/categories/" + BATH_CATEGORY + "/complete"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
        assertThat(readAtOf(UNREAD)).isNotNull();
    }

    @Test
    void 다른_카테고리를_완료해도_알림은_그대로다() throws Exception {
        mockMvc.perform(post("/api/v1/categories/" + FLOOR_CATEGORY + "/complete"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    // ---------- 생성 규칙 (backend-design 13.1) ----------

    @Test
    void 이미_미확인_알림이_있는_카테고리에는_새_알림을_만들지_않는다() {
        // 시드의 due 카테고리는 욕실뿐이고, 욕실에는 이미 미확인 알림이 있다
        assertThat(notificationService.generateDueNotifications()).isZero();
        assertThat(notificationRepository.countUnread(userId())).isEqualTo(1);
    }

    @Test
    void 미확인_알림을_읽은_뒤_여전히_due면_다음_실행에서_다시_생성된다() {
        notificationService.readAll();

        assertThat(notificationService.generateDueNotifications()).isEqualTo(1);
        assertThat(notificationRepository.countUnread(userId())).isEqualTo(1);
        assertThat(notificationRepository.findRecent(userId(), 10).get(0).title())
                .isEqualTo("이번 주에는 욕실만 챙겨도 충분해요");
    }

    @Test
    void due가_아닌_카테고리에는_알림을_만들지_않는다() {
        notificationService.readAll();
        notificationService.generateDueNotifications();

        // 새로 생긴 알림은 욕실 하나뿐이다
        assertThat(notificationRepository.countUnread(userId())).isEqualTo(1);
    }

    private UUID userId() {
        return currentUserProvider.currentUserId();
    }

    private LocalDateTime readAtOf(UUID notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId()).orElseThrow().readAt();
    }
}
