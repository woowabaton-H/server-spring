package cleanloop.user.dto;

import cleanloop.user.User;
import java.time.OffsetDateTime;

public record UserResponse(
        String id,
        String name,
        String avatarText,
        String timezone,
        OffsetDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().toString(),
                user.name(),
                user.avatarText(),
                user.timezone().getId(),
                user.createdAt().atZone(user.timezone()).toOffsetDateTime()
        );
    }
}
