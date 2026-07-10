package cleanloop.selection.dto;

import java.time.OffsetDateTime;

public record SaveSelectionResponse(String selectionId, boolean isSaved, OffsetDateTime savedAt) {
}
