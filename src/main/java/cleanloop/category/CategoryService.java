package cleanloop.category;

import cleanloop.category.dto.CategoryPresetResponse;
import cleanloop.category.dto.CategoryResponse;
import cleanloop.category.dto.CreateCategoryRequest;
import cleanloop.category.dto.UpdateCategoryRequest;
import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.user.User;
import cleanloop.user.UserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    /** MVP 허용 주기. P1에서 직접 입력을 허용할 수 있다. */
    private static final Set<Integer> ALLOWED_CYCLE_DAYS = Set.of(3, 7, 14, 21, 28);

    private final CategoryRepository categoryRepository;
    private final CategoryPresetRepository presetRepository;
    private final CategoryStatusService statusService;
    private final UserService userService;
    private final Clock clock;

    public CategoryService(CategoryRepository categoryRepository,
                           CategoryPresetRepository presetRepository,
                           CategoryStatusService statusService,
                           UserService userService,
                           Clock clock) {
        this.categoryRepository = categoryRepository;
        this.presetRepository = presetRepository;
        this.statusService = statusService;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryPresetResponse> findPresets() {
        return presetRepository.findAllActive().stream()
                .map(CategoryPresetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        User user = userService.getMe();
        return categoryRepository.findActiveByUserId(user.id()).stream()
                .map(category -> toResponse(category, user))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        User user = userService.getMe();
        CleaningCategory category = request.isPresetBased()
                ? fromPreset(user.id(), request.presetKey())
                : fromDirectInput(user.id(), request);

        if (categoryRepository.existsActiveByName(user.id(), category.name())) {
            throw new ApiException(ErrorCode.CATEGORY_DUPLICATED,
                    "같은 이름의 활성 카테고리가 이미 있습니다.", Map.of("name", category.name()));
        }
        categoryRepository.insert(category);
        return toResponse(findOwned(category.id(), user.id()), user);
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
        User user = userService.getMe();
        CleaningCategory current = findOwned(categoryId, user.id());

        String name = request.name() != null ? request.name() : current.name();
        int cycleDays = request.cycleDays() != null ? validateCycleDays(request.cycleDays()) : current.cycleDays();
        String note = request.note() != null ? request.note() : current.note();

        if (categoryRepository.existsActiveByNameExcluding(user.id(), name, categoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_DUPLICATED,
                    "같은 이름의 활성 카테고리가 이미 있습니다.", Map.of("name", name));
        }
        categoryRepository.update(categoryId, name, cycleDays, note);
        return toResponse(findOwned(categoryId, user.id()), user);
    }

    @Transactional
    public void deactivate(UUID categoryId) {
        User user = userService.getMe();
        findOwned(categoryId, user.id());
        categoryRepository.deactivate(categoryId);
    }

    /**
     * 상태와 nextDueAt은 저장하지 않고 사용자 타임존 기준으로 매 조회 시 계산한다.
     */
    private CategoryResponse toResponse(CleaningCategory category, User user) {
        CategorySchedule schedule =
                statusService.scheduleOf(category.lastDoneAt(), category.cycleDays(), user.timezone());
        return CategoryResponse.of(category, schedule, user.timezone());
    }

    private CleaningCategory findOwned(UUID categoryId, UUID userId) {
        return categoryRepository.findActiveByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private CleaningCategory fromPreset(UUID userId, String presetKey) {
        CategoryPreset preset = presetRepository.findActiveByKey(presetKey)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_PRESET_NOT_FOUND,
                        ErrorCode.CATEGORY_PRESET_NOT_FOUND.defaultMessage(), Map.of("presetKey", presetKey)));

        if (categoryRepository.existsActiveByPresetKey(userId, presetKey)) {
            throw new ApiException(ErrorCode.CATEGORY_DUPLICATED,
                    "같은 프리셋의 활성 카테고리가 이미 있습니다.", Map.of("presetKey", presetKey));
        }
        return newCategory(userId, presetKey, preset.name(), preset.icon(), preset.cycleDays(), preset.note());
    }

    private CleaningCategory fromDirectInput(UUID userId, CreateCategoryRequest request) {
        if (request.name() == null || request.icon() == null || request.cycleDays() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "presetKey가 없으면 name, icon, cycleDays가 필요합니다.",
                    Map.of("required", "name, icon, cycleDays"));
        }
        return newCategory(userId, null, request.name(), request.icon(),
                validateCycleDays(request.cycleDays()), request.note());
    }

    /**
     * 새 카테고리는 생성 시각을 마지막 완료 시각으로 잡아, 첫 관리 시점이 주기만큼 뒤가 되게 한다.
     */
    private CleaningCategory newCategory(UUID userId, String presetKey, String name,
                                         String icon, int cycleDays, String note) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new CleaningCategory(
                UUID.randomUUID(), userId, presetKey, name, icon, cycleDays,
                now, note, categoryRepository.nextSortOrder(userId), true, now, now);
    }

    private int validateCycleDays(int cycleDays) {
        if (!ALLOWED_CYCLE_DAYS.contains(cycleDays)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "cycleDays는 3, 7, 14, 21, 28 중 하나여야 합니다.",
                    Map.of("cycleDays", cycleDays));
        }
        return cycleDays;
    }
}
