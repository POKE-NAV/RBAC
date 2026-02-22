package filters;

import interfaces.RoleAssignment;
import record.AssignmentMetadata;
import record.Permission;
import record.User;
import role.PermanentAssignment;
import role.Role;
import role.TemporaryAssignment;

import javax.smartcardio.TerminalFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AssignmentFilters {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignmnet -> assignmnet.user().username().toLowerCase().equals(username.toLowerCase());
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().getName().toLowerCase().equals(roleName.toLowerCase());
    }

    public static AssignmentFilter activeOnly() {
        return assignment -> assignment.isActive();
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        LocalDateTime tempDate = parseDate(date);
        return assignment -> {
            LocalDateTime assignedDate = parseDate(assignment.metadata().assignedAt());
            return assignedDate.isAfter(tempDate);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        LocalDateTime tempDate = parseDate(date);
        return assignment -> {
            if(!(assignment instanceof TemporaryAssignment)) {
                return false;
            }
            TemporaryAssignment temp = (TemporaryAssignment) assignment;
            LocalDateTime expiryDate = parseDate(temp.getExpiresAt());
            return expiryDate.isBefore(tempDate);
        };
    }

    private static LocalDateTime parseDate(String date) {
        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте: yyyy-MM-dd HH:mm", e);
        }
    }

    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ ФИЛЬТРОВ НАЗНАЧЕНИЙ\n");

        System.out.println("ШАГ 1: Создание тестовых пользователей\n");

        User admin = User.validate("admin", "Admin User", "admin@company.com");
        User john = User.validate("john_doe", "John Doe", "john@gmail.com");
        User jane = User.validate("jane_smith", "Jane Smith", "jane@gmail.com");

        System.out.println("Пользователи:");
        System.out.println(admin.format());
        System.out.println(john.format());
        System.out.println(jane.format());
        System.out.println();

        System.out.println("ШАГ 2: Создание прав и ролей\n");

        Permission readUsers = new Permission("READ", "users", "Can view users");
        Permission writeUsers = new Permission("WRITE", "users", "Can edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");

        Role adminRole = new Role("Administrator", "Full system access");
        adminRole.addPermissions(readUsers);
        adminRole.addPermissions(writeUsers);
        adminRole.addPermissions(deleteUsers);

        Role moderatorRole = new Role("Moderator", "Can manage users");
        moderatorRole.addPermissions(readUsers);
        moderatorRole.addPermissions(writeUsers);

        Role viewerRole = new Role("Viewer", "Read only access");
        viewerRole.addPermissions(readUsers);

        System.out.println("Роли:");
        System.out.println(adminRole.getName() + " (прав: " + adminRole.getPermissions().size() + ")");
        System.out.println(moderatorRole.getName() + " (прав: " + moderatorRole.getPermissions().size() + ")");
        System.out.println(viewerRole.getName() + " (прав: " + viewerRole.getPermissions().size() + ")");
        System.out.println();

        System.out.println("ШАГ 3: Создание тестовых назначений\n");

        // Текущая дата для метаданных
        String now = LocalDateTime.now().format(FORMATTER);
        String yesterday = LocalDateTime.now().minusDays(1).format(FORMATTER);
        String tomorrow = LocalDateTime.now().plusDays(1).format(FORMATTER);
        String nextWeek = LocalDateTime.now().plusDays(7).format(FORMATTER);
        String nextMonth = LocalDateTime.now().plusDays(30).format(FORMATTER);
        String lastMonth = LocalDateTime.now().minusDays(30).format(FORMATTER);

        // Метаданные с разными датами назначения
        AssignmentMetadata metaToday = new AssignmentMetadata(admin.username(), now, "Назначение сегодня");
        AssignmentMetadata metaYesterday = new AssignmentMetadata(admin.username(), yesterday, "Назначение вчера");
        AssignmentMetadata metaTomorrow = new AssignmentMetadata(admin.username(), tomorrow, "Назначение завтра");
        AssignmentMetadata metaByJohn = new AssignmentMetadata(john.username(), now, "Назначил John");
        AssignmentMetadata metaByJane = new AssignmentMetadata(jane.username(), now, "Назначил Jane");

        // Создаем разные назначения
        System.out.println("Создание назначений...");


        PermanentAssignment perm1 = new PermanentAssignment(john, moderatorRole, metaToday);
        PermanentAssignment perm2 = new PermanentAssignment(john, adminRole, metaYesterday);
        TemporaryAssignment temp1 = new TemporaryAssignment(jane, viewerRole, metaTomorrow, nextMonth, true);
        TemporaryAssignment temp2 = new TemporaryAssignment(john, adminRole, metaByJohn, nextWeek, false);
        TemporaryAssignment temp3 = new TemporaryAssignment(jane, moderatorRole, metaByJane, lastMonth, false
        );

        List<RoleAssignment> assignments = List.of(perm1, perm2, temp1, temp2, temp3);

        System.out.println("\nВсе созданные назначения:");
        for (RoleAssignment ra : assignments) {
            System.out.println("  " + ra.format());
            if (ra instanceof TemporaryAssignment) {
                TemporaryAssignment temp = (TemporaryAssignment) ra;
                System.out.println("      истекает: " + temp.getExpiresAt());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 1: Фильтры по пользователю\n");

        System.out.println("byUser(john) - назначения пользователя John:");
        AssignmentFilter filter1 = byUser(john);
        for (RoleAssignment ra : assignments) {
            if (filter1.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("byUsername(\"jane_smith\") - назначения Jane:");
        AssignmentFilter filter1_2 = byUsername("jane_smith");
        for (RoleAssignment ra : assignments) {
            if (filter1_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 2: Фильтры по роли\n");

        System.out.println("byRole(adminRole) - назначения роли Administrator:");
        AssignmentFilter filter2 = byRole(adminRole);
        for (RoleAssignment ra : assignments) {
            if (filter2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("byRoleName(\"Moderator\") - назначения роли Moderator:");
        AssignmentFilter filter2_2 = byRoleName("Moderator");
        for (RoleAssignment ra : assignments) {
            if (filter2_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 3: Фильтры по активности\n");

        System.out.println("activeOnly() - только активные назначения:");
        AssignmentFilter filter3 = activeOnly();
        for (RoleAssignment ra : assignments) {
            if (filter3.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("inactiveOnly() - только неактивные назначения:");
        AssignmentFilter filter3_2 = inactiveOnly();
        for (RoleAssignment ra : assignments) {
            if (filter3_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 4: Фильтры по типу назначения\n");

        System.out.println("byType(\"PERMANENT\") - постоянные назначения:");
        AssignmentFilter filter4 = byType(RoleAssignment.TYPE_PERMANENT);
        for (RoleAssignment ra : assignments) {
            if (filter4.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("byType(\"TEMPORARY\") - временные назначения:");
        AssignmentFilter filter4_2 = byType(RoleAssignment.TYPE_TEMPORARY);
        for (RoleAssignment ra : assignments) {
            if (filter4_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 5: Фильтр по назначившему\n");

        System.out.println("assignedBy(\"admin\") - назначения от admin:");
        AssignmentFilter filter5 = assignedBy("admin");
        for (RoleAssignment ra : assignments) {
            if (filter5.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("assignedBy(\"john_doe\") - назначения от John:");
        AssignmentFilter filter5_2 = assignedBy("john_doe");
        for (RoleAssignment ra : assignments) {
            if (filter5_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 6: Фильтр по дате назначения\n");

        System.out.println("assignedAfter(\"" + now + "\") - назначения после текущего момента:");
        AssignmentFilter filter6 = assignedAfter(now);
        for (RoleAssignment ra : assignments) {
            if (filter6.test(ra)) {
                System.out.println(ra.format() + " (дата: " + ra.metadata().assignedAt() + ")");
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 7: Фильтр по дате истечения\n");

        String nextWeekPlusOne = LocalDateTime.now().plusDays(8).format(FORMATTER);
        System.out.println("expiringBefore(\"" + nextWeekPlusOne + "\") - временные назначения, истекающие до следующей недели:");
        AssignmentFilter filter7 = expiringBefore(nextWeekPlusOne);
        for (RoleAssignment ra : assignments) {
            if (filter7.test(ra)) {
                TemporaryAssignment temp = (TemporaryAssignment) ra;
                System.out.println(ra.format() + " (истекает: " + temp.getExpiresAt() + ")");
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 8: Комбинации фильтров\n");

        System.out.println("activeOnly().and(byType(\"TEMPORARY\")) - активные временные назначения:");
        AssignmentFilter filter8 = activeOnly().and(byType(RoleAssignment.TYPE_TEMPORARY));
        for (RoleAssignment ra : assignments) {
            if (filter8.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("assignedBy(\"admin\").or(byUsername(\"jane_smith\")) - назначения от admin ИЛИ для Jane:");
        AssignmentFilter filter8_2 = assignedBy("admin").or(byUsername("jane_smith"));
        for (RoleAssignment ra : assignments) {
            if (filter8_2.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("(activeOnly().and(byType(\"TEMPORARY\"))).or(byType(\"PERMANENT\").and(byUsername(\"john_doe\")))");
        System.out.println("  - активные временные ИЛИ постоянные для John:");
        AssignmentFilter filter8_3 = (activeOnly().and(byType(RoleAssignment.TYPE_TEMPORARY)))
                .or(byType(RoleAssignment.TYPE_PERMANENT).and(byUsername("john_doe")));
        for (RoleAssignment ra : assignments) {
            if (filter8_3.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 9: Фильтры, которые ничего не находят\n");

        System.out.println("byUsername(\"nonexistent\") - несуществующий пользователь:");
        AssignmentFilter filter9 = byUsername("nonexistent");
        boolean found = false;
        for (RoleAssignment ra : assignments) {
            if (filter9.test(ra)) {
                System.out.println(ra.format());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  (нет назначений)");
        }
        System.out.println();

        System.out.println("ТЕСТ 10: Длинные цепочки фильтров\n");

        System.out.println("Цепочка: byUsername(\"john_doe\").and(activeOnly()).and(byType(\"PERMANENT\"))");
        AssignmentFilter filter10 = byUsername("john_doe")
                .and(activeOnly())
                .and(byType(RoleAssignment.TYPE_PERMANENT));
        for (RoleAssignment ra : assignments) {
            if (filter10.test(ra)) {
                System.out.println(ra.format());
            }
        }
        System.out.println();
    }
}
