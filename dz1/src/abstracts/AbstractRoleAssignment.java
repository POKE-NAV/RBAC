package abstracts;

import interfaces.RoleAssignment;
import record.AssignmentMetadata;
import record.User;
import role.Role;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        if (user == null) {
            throw new IllegalArgumentException("User не может быть null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata не может быть null");
        }

        this.assignmentId = "assign_" + UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    public String summary() {
        String type = assignmentType();
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String reason = metadata().reason().isEmpty() ? "Не указана" : metadata().reason();

        return String.format("[%s] %s назначена %s пользователем %s в %s\nПричина: %s\nСтатус: %s",
                type,
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                metadata().assignedAt(),
                reason,
                status
        );

}
