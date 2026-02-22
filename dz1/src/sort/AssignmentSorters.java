package sort;

import interfaces.RoleAssignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class AssignmentSorters {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Comparator<RoleAssignment> byUsername() {
        return (assignment1, assignment2) ->assignment1.user().username().compareTo(assignment2.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return (assignment1, assignment2) -> assignment1.role().getName().compareTo(assignment2.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return (assignment1, assignment2) -> {
            LocalDateTime date1 = parseDate(assignment1.metadata().assignedAt());
            LocalDateTime date2 = parseDate(assignment1.metadata().assignedAt());
            return date1.compareTo(date2);
        };
    }

    private static LocalDateTime parseDate(String date) {
        return LocalDateTime.parse(date, FORMATTER);
    }
}
