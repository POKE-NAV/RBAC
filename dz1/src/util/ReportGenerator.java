package util;

import interfaces.RoleAssignment;
import managers.AssignmentManager;
import managers.RoleManager;
import managers.UserManager;
import record.Permission;
import record.User;
import role.Role;
import role.TemporaryAssignment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportGenerator {
    private static final String LINE = "_";

    private String repeatString(String s, int count) {
        return s.repeat(count);
    }

    private String truncate(String str, int length) {
        if (str == null) return "";
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    public String generateUserReportParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(" ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ (ПАРАЛЛЕЛЬНЫЙ) ");
        report.append("\n").append(repeatString(LINE, 80)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("В системе нет пользователей\n");
            return report.toString();
        }

        // Параллельная обработка пользователей
        String userData = users.parallelStream()
                .map(user -> formatUserForReport(user, assignmentManager))
                .collect(Collectors.joining(repeatString(LINE, 80) + "\n\n"));

        report.append(userData);
        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(String.format("ИТОГО: %d пользователей\n", users.size()));

        return report.toString();
    }

    private String formatUserForReport(User user, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(repeatString(LINE, 80)).append("\n");
        sb.append(String.format("Username: %s\n", user.username()));
        sb.append(String.format("Full Name: %s\n", user.fullname()));
        sb.append(String.format("Email: %s\n", user.email()));
        sb.append(repeatString(LINE, 80)).append("\n");

        List<RoleAssignment> assignments = assignmentManager.findByUser(user);
        if (!assignments.isEmpty()) {
            sb.append("Назначенные роли:\n");
            int activeCount = 0;
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "Активна" : "Неактивна";
                String type = ra.assignmentType().equals(RoleAssignment.TYPE_PERMANENT) ? "Постоянная" : "Временная";
                sb.append(String.format("  - %s [%s] %s\n", ra.role().getName(), type, status));

                if (ra instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) ra;
                    sb.append(String.format("    Истекает: %s (осталось: %s)\n",
                            temp.getExpiresAt(), temp.getTimeRemaining()));
                }
                if (ra.isActive()) activeCount++;
            }
            sb.append(String.format("Всего ролей: %d (активных: %d)\n", assignments.size(), activeCount));
        }

        Set<Permission> permissions = assignmentManager.getUserPermissions(user);
        if (!permissions.isEmpty()) {
            sb.append("Права доступа:\n");
            for (Permission p : permissions) {
                sb.append(String.format("  - %s on %s: %s\n", p.name(), p.resource(), p.description()));
            }
        }

        return sb.toString();
    }

    public String generateRoleReportParallel(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(" ОТЧЁТ ПО РОЛЯМ (ПАРАЛЛЕЛЬНЫЙ) ");
        report.append("\n").append(repeatString(LINE, 80)).append("\n\n");

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            report.append("В системе нет ролей\n");
            return report.toString();
        }

        // Параллельная обработка ролей
        String rolesData = roles.parallelStream()
                .map(role -> formatRoleForReport(role, assignmentManager))
                .collect(Collectors.joining(repeatString(LINE, 80) + "\n\n"));

        report.append(rolesData);
        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(String.format("ИТОГО: %d ролей\n", roles.size()));

        return report.toString();
    }

    private String formatRoleForReport(Role role, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(repeatString(LINE, 80)).append("\n");
        sb.append(String.format("Название роли: %s\n", role.getName()));
        sb.append(String.format("Описание: %s\n", role.getDescription()));
        sb.append(String.format("ID: %s\n", role.getId()));
        sb.append(String.format("Количество прав: %d\n", role.getPermissions().size()));
        sb.append(repeatString(LINE, 80)).append("\n");

        // Права роли
        Set<Permission> permissions = role.getPermissions();
        if (!permissions.isEmpty()) {
            sb.append("Права:\n");
            for (Permission p : permissions) {
                sb.append(String.format("  - %s on %s: %s\n", p.name(), p.resource(), p.description()));
            }
        } else {
            sb.append("Права: нет\n");
        }

        // Пользователи с этой ролью
        List<RoleAssignment> assignments = assignmentManager.findByRole(role);
        if (!assignments.isEmpty()) {
            sb.append("Назначена пользователям:\n");
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "Активна" : "Неактивна";
                sb.append(String.format("  - %s [%s]\n", ra.user().username(), status));
            }
            long activeCount = assignments.stream().filter(RoleAssignment::isActive).count();
            sb.append(String.format("Всего назначений: %d (активных: %d)\n", assignments.size(), activeCount));
        } else {
            sb.append("Назначена пользователям: нет\n");
        }

        return sb.toString();
    }

    public String generatePermissionMatrixParallel(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("\n").append(repeatString(LINE, 100)).append("\n");
        report.append(" МАТРИЦА ПРАВ ДОСТУПА (ПАРАЛЛЕЛЬНАЯ) ");
        report.append("\n").append(repeatString(LINE, 100)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("В системе нет пользователей\n");
            return report.toString();
        }

        // Параллельный сбор всех уникальных ресурсов
        Set<String> allResources = users.parallelStream()
                .flatMap(user -> assignmentManager.getUserPermissions(user).stream())
                .map(Permission::resource)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> resources = new ArrayList<>(allResources);

        if (resources.isEmpty()) {
            report.append("Нет доступных ресурсов\n");
            return report.toString();
        }

        // Заголовок таблицы
        report.append(repeatString(LINE, 20)).append(" ");
        for (String resource : resources) {
            report.append(repeatString(LINE, 15)).append(" ");
        }
        report.append("\n");

        report.append(String.format("%-20s ", "Пользователь"));
        for (String resource : resources) {
            report.append(String.format("%-15s ", truncate(resource, 15)));
        }
        report.append("\n");

        report.append(repeatString(LINE, 20)).append(" ");
        for (int i = 0; i < resources.size(); i++) {
            report.append(repeatString(LINE, 15));
            if (i < resources.size() - 1) report.append(" ");
        }
        report.append("\n");

        // Параллельная обработка пользователей для матрицы
        String matrixData = users.parallelStream()
                .map(user -> formatUserPermissionsRow(user, resources, assignmentManager))
                .collect(Collectors.joining());

        report.append(matrixData);

        report.append(repeatString(LINE, 20)).append(" ");
        for (int i = 0; i < resources.size(); i++) {
            report.append(repeatString(LINE, 15));
            if (i < resources.size() - 1) report.append(" ");
        }
        report.append("\n\n");

        // Легенда
        report.append("Легенда:\n");
        report.append("  READ    - Просмотр\n");
        report.append("  WRITE   - Редактирование\n");
        report.append("  DELETE  - Удаление\n");
        report.append("  VIEW    - Просмотр отчетов\n\n");

        report.append(repeatString(LINE, 100)).append("\n");
        report.append(String.format("ИТОГО: %d пользователей, %d ресурсов\n",
                users.size(), resources.size()));
        report.append(repeatString(LINE, 100)).append("\n");

        return report.toString();
    }

    private String formatUserPermissionsRow(User user, List<String> resources, AssignmentManager assignmentManager) {
        StringBuilder row = new StringBuilder();

        Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
        Map<String, Set<String>> userPermissionsByResource = new ConcurrentHashMap<>();

        for (Permission p : userPerms) {
            userPermissionsByResource
                    .computeIfAbsent(p.resource(), k -> ConcurrentHashMap.newKeySet())
                    .add(p.name());
        }

        row.append(String.format("%-20s ", truncate(user.username(), 20)));

        for (String resource : resources) {
            Set<String> perms = userPermissionsByResource.getOrDefault(resource, new HashSet<>());
            if (perms.isEmpty()) {
                row.append(repeatString(" ", 15));
            } else {
                String permStr = String.join("/", perms);
                row.append(String.format("%-15s ", truncate(permStr, 15)));
            }
        }
        row.append("\n");

        return row.toString();
    }

    public void exportToFile(String report, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
            System.out.println("Отчёт сохранён в файл: " + filename);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении отчёта: " + e.getMessage());
        }
    }
}