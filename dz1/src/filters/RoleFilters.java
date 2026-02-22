package filters;

import record.Permission;
import role.Role;

import java.util.List;

public class RoleFilters {
    public static RoleFilter byName(String name) {
        return role -> role.getName().toLowerCase().equals(name.toLowerCase());
    }

    public static RoleFilter byNameContains(String substring) {
        return role -> role.getName().toLowerCase().contains(substring.toLowerCase());
    }

    public static RoleFilter hasPermission(Permission permission) {
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) {
        return role -> role.hasPermission(permissionName, resource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }

    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ ФИЛЬТРОВ РОЛЕЙ\n");

        System.out.println("ШАГ 1: Создание тестовых прав\n");

        Permission readUsers = new Permission("READ", "users", "Can view users");
        Permission writeUsers = new Permission("WRITE", "users", "Can edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission readReports = new Permission("READ", "reports", "Can view reports");
        Permission writeReports = new Permission("WRITE", "reports", "Can edit reports");
        Permission deleteReports = new Permission("DELETE", "reports", "Can delete reports");
        Permission readSettings = new Permission("READ", "settings", "Can view settings");

        System.out.println("Созданные права:");
        System.out.println(readUsers.format());
        System.out.println(writeUsers.format());
        System.out.println(deleteUsers.format());
        System.out.println(readReports.format());
        System.out.println(writeReports.format());
        System.out.println(deleteReports.format());
        System.out.println(readSettings.format());
        System.out.println();

        System.out.println("ШАГ 2: Создание тестовых ролей\n");

        // Администратор
        Role adminRole = new Role("Administrator", "Full system access");
        adminRole.addPermissions(readUsers);
        adminRole.addPermissions(writeUsers);
        adminRole.addPermissions(deleteUsers);
        adminRole.addPermissions(readReports);
        adminRole.addPermissions(writeReports);
        adminRole.addPermissions(deleteReports);
        adminRole.addPermissions(readSettings);

        // Модератор пользователей
        Role userModerator = new Role("User Moderator", "Can manage users");
        userModerator.addPermissions(readUsers);
        userModerator.addPermissions(writeUsers);
        userModerator.addPermissions(deleteUsers);

        // Модератор отчетов
        Role reportModerator = new Role("Report Moderator", "Can manage reports");
        reportModerator.addPermissions(readReports);
        reportModerator.addPermissions(writeReports);
        reportModerator.addPermissions(deleteReports);

        // Viewer
        Role viewer = new Role("Viewer", "Read only access");
        viewer.addPermissions(readUsers);
        viewer.addPermissions(readReports);
        viewer.addPermissions(readSettings);

        // Гость
        Role guest = new Role("Guest", "Limited access");
        guest.addPermissions(readUsers);

        List<Role> roles = List.of(adminRole, userModerator, reportModerator, viewer, guest);

        System.out.println("Тестовые роли:");
        for (Role role : roles) {
            System.out.println("  " + role.getName() + " (ID: " + role.getId() +
                    ", прав: " + role.getPermissions().size() + ")");
        }
        System.out.println();

        // ========== ТЕСТ 1: byName ==========
        System.out.println("ТЕСТ 1: byName(\"Administrator\")");
        RoleFilter filter1 = byName("Administrator");
        for (Role role : roles) {
            if (filter1.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 1.2: byName(\"Admin\") - нет такой роли");
        RoleFilter filter1_2 = byName("Admin");
        boolean found = false;
        for (Role role : roles) {
            if (filter1_2.test(role)) {
                System.out.println(role.getName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  (нет ролей)");
        }
        System.out.println();

        System.out.println("ТЕСТ 2: byNameContains(\"moderator\") - без учета регистра");
        RoleFilter filter2 = byNameContains("moderator");
        for (Role role : roles) {
            if (filter2.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 2.2: byNameContains(\"R\") - поиск части слова");
        RoleFilter filter2_2 = byNameContains("ER");
        for (Role role : roles) {
            if (filter2_2.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 3: hasPermission(право DELETE на users)");
        RoleFilter filter3 = hasPermission(deleteUsers);
        for (Role role : roles) {
            if (filter3.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 4: hasPermission(\"READ\", \"reports\")");
        RoleFilter filter4 = hasPermission("READ", "reports");
        for (Role role : roles) {
            if (filter4.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 4.2: hasPermission(\"WRITE\", \"settings\") - нет такого права");
        RoleFilter filter4_2 = hasPermission("WRITE", "settings");
        for (Role role : roles) {
            if (filter4_2.test(role)) {
                System.out.println(role.getName());
            }
        }
        if (!found) {
            System.out.println("  (нет ролей)");
        }
        System.out.println();

        System.out.println("ТЕСТ 5: hasAtLeastNPermissions(3)");
        RoleFilter filter5 = hasAtLeastNPermissions(3);
        for (Role role : roles) {
            if (filter5.test(role)) {
                System.out.println(role.getName() + " (прав: " +
                        role.getPermissions().size() + ")");
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 5.2: hasAtLeastNPermissions(5)");
        RoleFilter filter5_2 = hasAtLeastNPermissions(5);
        for (Role role : roles) {
            if (filter5_2.test(role)) {
                System.out.println(role.getName() + " (прав: " +
                        role.getPermissions().size() + ")");
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 6: hasPermission(\"DELETE\", \"users\") AND hasAtLeastNPermissions(3)");
        RoleFilter filter6 = hasPermission("DELETE", "users")
                .and(hasAtLeastNPermissions(3));
        for (Role role : roles) {
            if (filter6.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 7: hasPermission(\"WRITE\", \"reports\") OR hasPermission(\"READ\", \"settings\")");
        RoleFilter filter7 = hasPermission("WRITE", "reports")
                .or(hasPermission("READ", "settings"));
        for (Role role : roles) {
            if (filter7.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 8: (hasPermission(\"READ\", \"users\") AND hasAtLeastNPermissions(2))");
        System.out.println("        OR");
        System.out.println("        (byNameContains(\"moderator\") AND hasPermission(\"DELETE\", \"reports\"))");

        RoleFilter filter8 = (hasPermission("READ", "users").and(hasAtLeastNPermissions(2)))
                .or(byNameContains("moderator").and(hasPermission("DELETE", "reports")));

        for (Role role : roles) {
            if (filter8.test(role)) {
                System.out.println(role.getName());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 9: hasPermission(\"EXECUTE\", \"scripts\") - несуществующее право");
        RoleFilter filter9 = hasPermission("EXECUTE", "scripts");
        found = false;
        for (Role role : roles) {
            if (filter9.test(role)) {
                System.out.println(role.getName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  (нет ролей с таким правом)");
        }
        System.out.println();
    }
}
