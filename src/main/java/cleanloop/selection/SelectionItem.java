package cleanloop.selection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 브랜드 셀렉션 항목.
 * API가 노출하는 식별자는 slug이고, UUID인 id는 내부 참조에만 쓴다.
 */
public record SelectionItem(
        UUID id,
        String slug,
        String type,
        String category,
        String title,
        String label,
        String priceText,
        String affiliateText,
        String reason,
        String fitFor,
        String notice,
        String imageUrl,
        String ratingText,
        String reviewCountText,
        boolean highlighted,
        String externalUrl,
        String status,
        int sortOrder,
        LocalDateTime createdAt
) {
}
