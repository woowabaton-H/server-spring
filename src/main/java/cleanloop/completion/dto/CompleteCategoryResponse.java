package cleanloop.completion.dto;

import cleanloop.category.dto.CategoryResponse;

public record CompleteCategoryResponse(
        CategoryResponse category,
        CompletionLogResponse log,
        String toastMessage
) {
}
