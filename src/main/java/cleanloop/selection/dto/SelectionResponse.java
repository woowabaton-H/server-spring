package cleanloop.selection.dto;

import cleanloop.selection.SelectionItem;
import cleanloop.selection.SelectionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * id는 DB의 UUID가 아니라 외부 노출 키인 slug다.
 *
 * <p>imageUrl, ratingText, reviewCountText, tags는 셀렉션 카드가 쓰는 표시 정보라 목록에도 담는다.
 * notice와 checks는 판단 근거에 가까워 상세 조회에서만 채우고, 목록 응답에서는 키 자체가 빠진다.
 */
public record SelectionResponse(
        String id,
        String type,
        String typeLabel,
        String category,
        String title,
        String label,
        boolean isHighlighted,
        String priceText,
        String affiliateText,
        String imageUrl,
        String ratingText,
        String reviewCountText,
        List<String> tags,
        String reason,
        String fitFor,
        @JsonInclude(JsonInclude.Include.NON_NULL) String notice,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<String> checks,
        boolean isSaved,
        List<ProviderResponse> providers
) {

    /** 목록 응답. 고지 문구, 확인 항목, 제공업체는 상세에서만 내려준다. */
    public static SelectionResponse ofListItem(SelectionItem item, List<String> tags, boolean saved) {
        return of(item, tags, saved, null, null, List.of());
    }

    public static SelectionResponse ofDetail(SelectionItem item, List<String> tags, boolean saved,
                                             List<String> checks, List<ProviderResponse> providers) {
        return of(item, tags, saved, item.notice(), checks, providers);
    }

    private static SelectionResponse of(SelectionItem item, List<String> tags, boolean saved,
                                        String notice, List<String> checks, List<ProviderResponse> providers) {
        return new SelectionResponse(
                item.slug(),
                item.type(),
                SelectionType.labelOf(item.type()),
                item.category(),
                item.title(),
                item.label(),
                item.highlighted(),
                item.priceText(),
                item.affiliateText(),
                item.imageUrl(),
                item.ratingText(),
                item.reviewCountText(),
                tags,
                item.reason(),
                item.fitFor(),
                notice,
                checks,
                saved,
                providers);
    }
}
