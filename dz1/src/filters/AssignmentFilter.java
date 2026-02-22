package filters;

import interfaces.RoleAssignment;

@FunctionalInterface
public interface AssignmentFilter {
    boolean test(RoleAssignment role);

    default AssignmentFilter and(AssignmentFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("AssignmentFilter не может быть равен null");
        }
        return role -> this.test(role) && other.test(role);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("AssignmentFilter не может быть равен null");
        }
        return role -> this.test(role) || other.test(role);
    }
}
