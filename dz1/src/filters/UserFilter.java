package filters;

import record.User;

import java.util.List;

@FunctionalInterface
public interface UserFilter {
    boolean test(User user);

    default UserFilter and(UserFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("UserFilter не может быть равен null");
        }

        return user -> this.test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("UserFilter не может быть равен null");
        }

        return user -> this.test(user) || other.test(user);
    }
}
