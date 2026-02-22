package filters;

import role.Role;

@FunctionalInterface
public interface RoleFilter {
    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("UserFilter не может быть равен null");
        }
        return role -> this.test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("UserFilter не может быть равен null");
        }
        return role -> this.test(role) || other.test(role);
    }
}
