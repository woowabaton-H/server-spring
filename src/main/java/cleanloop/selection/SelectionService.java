package cleanloop.selection;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.common.page.CursorCodec;
import cleanloop.common.user.CurrentUserProvider;
import cleanloop.selection.dto.ExternalViewRequest;
import cleanloop.selection.dto.ExternalViewResponse;
import cleanloop.selection.dto.ProviderResponse;
import cleanloop.selection.dto.SaveSelectionResponse;
import cleanloop.selection.dto.SelectionResponse;
import cleanloop.user.User;
import cleanloop.user.UserService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelectionService {

    private static final Logger log = LoggerFactory.getLogger(SelectionService.class);

    /** 외부 URL이 준비되지 않아도 최종 조건 확인 고지는 항상 함께 노출한다. */
    private static final String DEFAULT_NOTICE = "외부 페이지에서 최종 가격과 조건을 확인하세요.";

    private final SelectionRepository selectionRepository;
    private final ProviderOptionRepository providerOptionRepository;
    private final SavedSelectionRepository savedSelectionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    public SelectionService(SelectionRepository selectionRepository,
                            ProviderOptionRepository providerOptionRepository,
                            SavedSelectionRepository savedSelectionRepository,
                            CurrentUserProvider currentUserProvider,
                            UserService userService) {
        this.selectionRepository = selectionRepository;
        this.providerOptionRepository = providerOptionRepository;
        this.savedSelectionRepository = savedSelectionRepository;
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public Page findAll(String category, String type, String cursor, int limit) {
        UUID userId = currentUserProvider.currentUserId();
        List<SelectionItem> items = selectionRepository.findPage(category, type, decodeCursor(cursor), limit);

        boolean hasNext = items.size() > limit;
        if (hasNext) {
            items = items.subList(0, limit);
        }
        String nextCursor = hasNext ? encodeCursor(items.get(items.size() - 1)) : null;

        Set<UUID> savedIds = savedSelectionRepository.findSavedItemIds(userId);
        List<SelectionResponse> responses = items.stream()
                .map(item -> SelectionResponse.ofListItem(item, savedIds.contains(item.id())))
                .toList();
        return new Page(responses, nextCursor);
    }

    @Transactional(readOnly = true)
    public SelectionResponse findBySlug(String slug) {
        UUID userId = currentUserProvider.currentUserId();
        SelectionItem item = findPublished(slug);

        List<ProviderResponse> providers = providerOptionRepository.findActiveBySelectionItemId(item.id()).stream()
                .map(ProviderResponse::from)
                .toList();
        boolean saved = savedSelectionRepository.findSavedAt(userId, item.id()).isPresent();
        return SelectionResponse.ofDetail(item, saved, providers);
    }

    @Transactional
    public SaveSelectionResponse save(String slug) {
        User user = userService.getMe();
        SelectionItem item = findPublished(slug);

        LocalDateTime savedAt = savedSelectionRepository.save(user.id(), item.id());
        return new SaveSelectionResponse(item.slug(), true, savedAt.atZone(user.timezone()).toOffsetDateTime());
    }

    /** 저장돼 있지 않아도 성공으로 본다. 반복 호출해도 결과가 같다. */
    @Transactional
    public void unsave(String slug) {
        UUID userId = currentUserProvider.currentUserId();
        SelectionItem item = findPublished(slug);
        savedSelectionRepository.delete(userId, item.id());
    }

    @Transactional(readOnly = true)
    public List<SelectionResponse> findSaved() {
        UUID userId = currentUserProvider.currentUserId();
        List<UUID> orderedIds = savedSelectionRepository.findSavedItemIdsOrderBySavedAtDesc(userId);

        Map<UUID, SelectionItem> itemsById = selectionRepository.findPublishedByIds(orderedIds).stream()
                .collect(Collectors.toMap(SelectionItem::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        // 저장 시각 최신순은 SQL이 정했다. in 절 결과 순서는 보장되지 않으므로 여기서 다시 세운다.
        return orderedIds.stream()
                .map(itemsById::get)
                .filter(java.util.Objects::nonNull)
                .map(item -> SelectionResponse.ofListItem(item, true))
                .toList();
    }

    public int countSaved(UUID userId) {
        return savedSelectionRepository.countByUserId(userId);
    }

    /**
     * 앱 내부에서 결제나 예약 확정을 하지 않는다. 외부로 나가는 클릭을 기록하고 이동 정보만 돌려준다.
     * 분석 이벤트 테이블이 없는 단계이므로 로그로만 남긴다.
     */
    @Transactional(readOnly = true)
    public ExternalViewResponse recordExternalView(String slug, ExternalViewRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        SelectionItem item = findPublished(slug);

        String providerId = request != null ? request.providerId() : null;
        String externalUrl = item.externalUrl();
        if (providerId != null) {
            ProviderOption provider = providerOptionRepository
                    .findActiveById(parseProviderId(providerId), item.id())
                    .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
            externalUrl = provider.externalUrl();
        }

        log.info("external_view_clicked userId={} selection={} provider={}", userId, slug, providerId);
        return new ExternalViewResponse(item.slug(), providerId, externalUrl, noticeOf(item));
    }

    private String noticeOf(SelectionItem item) {
        return item.notice() != null ? item.notice() : DEFAULT_NOTICE;
    }

    private UUID parseProviderId(String providerId) {
        try {
            return UUID.fromString(providerId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_FOUND);
        }
    }

    private SelectionItem findPublished(String slug) {
        return selectionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.SELECTION_NOT_FOUND));
    }

    private String encodeCursor(SelectionItem item) {
        return CursorCodec.encode(
                item.highlighted() + "|" + item.sortOrder() + "|" + item.createdAt() + "|" + item.id());
    }

    private SelectionRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = CursorCodec.decode(cursor).split("\\|", 4);
        if (parts.length != 4) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
        try {
            return new SelectionRepository.Cursor(
                    Boolean.parseBoolean(parts[0]),
                    Integer.parseInt(parts[1]),
                    LocalDateTime.parse(parts[2]),
                    UUID.fromString(parts[3]));
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
    }

    public record Page(List<SelectionResponse> selections, String nextCursor) {
    }
}
