package cleanloop.selection;

import java.util.UUID;

/**
 * 서비스/구독형 셀렉션에 연결되는 제공업체 후보.
 */
public record ProviderOption(
        UUID id,
        UUID selectionItemId,
        String name,
        String ratingText,
        String priceText,
        String note,
        String externalUrl,
        int sortOrder
) {
}
