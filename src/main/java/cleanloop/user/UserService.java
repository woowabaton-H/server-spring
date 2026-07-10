package cleanloop.user;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.common.user.CurrentUserProvider;
import cleanloop.user.dto.UpdateMeRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserService(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public User getMe() {
        return findCurrentUser();
    }

    @Transactional
    public User updateMe(UpdateMeRequest request) {
        User current = findCurrentUser();
        String name = request.name() != null ? request.name() : current.name();
        String avatarText = request.avatarText() != null ? request.avatarText() : current.avatarText();

        userRepository.updateProfile(current.id(), name, avatarText);
        return findCurrentUser();
    }

    private User findCurrentUser() {
        UUID userId = currentUserProvider.currentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
