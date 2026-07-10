package cleanloop.selection.dto;

import cleanloop.selection.ProviderOption;

public record ProviderResponse(
        String id,
        String name,
        String ratingText,
        String priceText,
        String note
) {

    public static ProviderResponse from(ProviderOption provider) {
        return new ProviderResponse(
                provider.id().toString(),
                provider.name(),
                provider.ratingText(),
                provider.priceText(),
                provider.note());
    }
}
