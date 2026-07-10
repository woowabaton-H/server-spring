package cleanloop.selection.dto;

import cleanloop.selection.SelectionItem;
import cleanloop.selection.SelectionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * id는 DB의 UUID가 아니라 외부 노출 키인 slug다.
 * notice는 상세 조회에서만 채우므로, 목록 응답에서는 키 자체가 빠진다.
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
        String reason,
        String fitFor,
        @JsonInclude(JsonInclude.Include.NON_NULL) String notice,
        boolean isSaved,
        List<ProviderResponse> providers
) {

    /** 목록 응답. 고지 문구와 제공업체는 상세에서만 내려준다. */
    public static SelectionResponse ofListItem(SelectionItem item, boolean saved) {
        return of(item, saved, null, List.of());
    }

    public static SelectionResponse ofDetail(SelectionItem item, boolean saved, List<ProviderResponse> providers) {
        return of(item, saved, item.notice(), providers);
    }

    private static SelectionResponse of(SelectionItem item, boolean saved,
                                        String notice, List<ProviderResponse> providers) {
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
                item.reason(),
                item.fitFor(),
                notice,
                saved,
                providers);
    }
}
