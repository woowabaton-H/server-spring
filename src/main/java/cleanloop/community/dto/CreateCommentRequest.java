package cleanloop.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "body는 필수입니다.")
        @Size(max = 1000, message = "body는 1000자 이하여야 합니다.")
        String body
) {
}
