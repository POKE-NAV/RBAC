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
}
