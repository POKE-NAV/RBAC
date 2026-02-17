package interfaces;
import record.AssignmentMetadata;
import record.User;
import role.Role;
public interface RoleAssignment {

    String TYPE_PERMANENT = "PERMANENT";
    String TYPE_TEMPORARY = "TEMPORARY";

    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();

    default String format() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        return String.format("%s | %s | %s | %s | %s",
                user().username(),
                role().getName(),
                metadata().assignedBy(),
                metadata().assignedAt(),
                status
        );
    }
    default boolean isPermanent() {
        return TYPE_PERMANENT.equals(assignmentType());
    }

    default boolean isTemporary() {
        return TYPE_TEMPORARY.equals(assignmentType());
    }
}
