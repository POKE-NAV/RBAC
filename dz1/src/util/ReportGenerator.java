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

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager){
        StringBuilder report = new StringBuilder();
        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(" Отчет по пользователям ");
        report.append("\n").append(repeatString(LINE, 80)).append("\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("В системе нет пользователей");
            return report.toString();
        }

        for (User user : users) {
            report.append("+").append(repeatString(LINE, 78)).append("+\n");
            report.append(String.format("│ %-20s │ %-30s │ %-24s │\n",
                    "Username", "Full Name", "Email"));
            report.append("+").append(repeatString(LINE, 78)).append("+\n");
            report.append(String.format("│ %-20s │ %-30s │ %-24s │\n",
                    truncate(user.username(), 20),
                    truncate(user.fullname(), 30),
                    truncate(user.email(), 24)));
            report.append("+").append(repeatString(LINE, 78)).append("+\n");

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);

            if (assignments.isEmpty()) {
                report.append("Нет назначенных ролей \n\n");
            } else {
                report.append("Назначенные роли: \n");
                int activeCount = 0;
                for (RoleAssignment ra : assignments) {
                    String status = ra.isActive() ? "Активна" : "Неактивна";
                    String type = ra.assignmentType().equals(RoleAssignment.TYPE_PERMANENT) ? "Постоянная" : "Временная";
                    report.append(String.format("%-20s [%s] %s",ra.role().getName(), type, status));

                    if (ra instanceof TemporaryAssignment) {
                        TemporaryAssignment temp = (TemporaryAssignment) ra;
                        report.append(String.format("Истекает: %s (осталось %s)\n",
                                temp.getExpiresAt(), temp.getTimeRemaining()));
                    }
                    if (ra.isActive()) {
                        activeCount++;
                    }
                }
                report.append(String.format("\n Всего ролей : %d (активных %d)\n\n",
                        assignments.size(), activeCount));
            }

            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            if (!permissions.isEmpty()) {
                report.append("Права доступа: \n");
                for (Permission p : permissions) {
                    report.append(String.format("%s on %s: %s\n", p.name(), p.resource(), p.description()));
                }
                report.append("\n");
            }
            report.append(repeatString(LINE, 80)).append("\n\n");
        }
        report.append(String.format("ИТОГО: %d пользователей\n", users.size()));
        return report.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();
        report.append("\n").append(repeatString(LINE, 80)).append("\n");
        report.append(" Отчет по ролям ");
        report.append("\n").append(repeatString(LINE, 80)).append("\n");

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            report.append("В системе нет ролей");
            return report.toString();
        }

        report.append("+").append(repeatString(LINE, 28)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+\n");
        report.append(String.format("│ %-28s │ %-15s │ %-15s │ %-15s │\n",
                "Название роли", "Всего польз.", "Активных", "Прав"));
        report.append("├").append(repeatString(LINE, 28)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+\n");

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            int totalUsers = assignments.size();
            long activeUsers = assignments.stream().filter(RoleAssignment::isActive).count();

            report.append(String.format("│ %-28s │ %15d │ %15d │ %15d │\n",
                    truncate(role.getName(), 28),
                    totalUsers,
                    activeUsers,
                    role.getPermissions().size()));
        }
        report.append("+").append(repeatString(LINE, 28)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+")
                .append(repeatString(LINE, 15)).append("+\n\n");

        report.append("Детальная информация:\n");
        report.append(repeatString(LINE, 80)).append("\n\n");

        for (Role role : roles) {
            report.append("+").append(repeatString(LINE, 78)).append("+\n");
            report.append(String.format("│ РОЛЬ: %-40s │\n", role.getName()));
            report.append(String.format("│ Описание: %-70s │\n", truncate(role.getDescription(), 70)));
            report.append(String.format("│ ID: %-73s │\n", role.getId()));
            report.append("+").append(repeatString(LINE, 78)).append("+\n");

            Set<Permission> permissions = role.getPermissions();
            if (permissions.isEmpty()) {
                report.append("│ Нет прав                                                 │\n");
            } else {
                report.append("│ Права:                                                    │\n");
                for (Permission p : permissions) {
                    report.append(String.format("│   • %-6s on %-10s: %-45s │\n",
                            p.name(), p.resource(), truncate(p.description(), 45)));
                }
            }
            report.append("+").append(repeatString(LINE, 78)).append("+\n");

            List<RoleAssignment> roleAssignments = assignmentManager.findByRole(role);
            if (roleAssignments.isEmpty()) {
                report.append("│ Нет назначений                                           │\n");
            } else {
                report.append("│ Назначена пользователям:                                   │\n");
                for (RoleAssignment ra : roleAssignments) {
                    String status = ra.isActive() ? "Активна" : "Неактивна";
                    String type = ra.assignmentType().equals(RoleAssignment.TYPE_PERMANENT) ? "P" : "T";

                    if (ra instanceof TemporaryAssignment) {
                        TemporaryAssignment temp = (TemporaryAssignment) ra;
                        report.append(String.format("│   %s %-15s [%s] до %-16s           │\n",
                                status, ra.user().username(), type,
                                temp.getExpiresAt()));
                    } else {
                        report.append(String.format("│   %s %-15s [%s]                         │\n",
                                status, ra.user().username(), type));
                    }
                }
            }

            report.append("+").append(repeatString(LINE, 78)).append("+\n\n");
        }

        report.append(repeatString(LINE, 80)).append("\n");
        report.append(String.format("ИТОГО: %d ролей\n", roles.size()));
        report.append(repeatString(LINE, 80)).append("\n");

        return report.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append("\n").append(repeatString(LINE, 100)).append("\n");
        report.append("Матрица прав доступа\n");
        report.append(repeatString(LINE, 100)).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            report.append("В системе нет пользователей\n");
            return report.toString();
        }

        Set<String> allResources = new TreeSet<>();
        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            for (Permission p : perms) {
                allResources.add(p.resource());
            }
        }

        List<String> resources = new ArrayList<>(allResources);

        if (resources.isEmpty()) {
            report.append("Нет доступных ресурсов\n");
            return report.toString();
        }

        report.append("+").append(repeatString(LINE, 20)).append("+");
        for (int i = 0; i < resources.size(); i++) {
            report.append(repeatString(LINE, 15));
            if (i < resources.size() - 1) {
                report.append("+");
            }
        }
        report.append("+\n");

        report.append(String.format("│ %-20s │", "Пользователь"));
        for (String resource : resources) {
            report.append(String.format(" %-14s │", truncate(resource, 14)));
        }
        report.append("\n");

        report.append("+").append(repeatString(LINE, 20)).append("+");
        for (int i = 0; i < resources.size(); i++) {
            report.append(repeatString(LINE, 15));
            if (i < resources.size() - 1) {
                report.append("+");
            }
        }
        report.append("+\n");

        for (User user : users) {
            report.append(String.format("│ %-20s │", truncate(user.username(), 20)));

            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
            Map<String, Set<String>> userPermissionsByResource = new HashMap<>();

            for (Permission p : userPerms) {
                userPermissionsByResource
                        .computeIfAbsent(p.resource(), k -> new TreeSet<>())
                        .add(p.name());
            }

            for (String resource : resources) {
                Set<String> perms = userPermissionsByResource.getOrDefault(resource, new TreeSet<>());
                if (perms.isEmpty()) {
                    report.append(" ".repeat(15)).append("│");
                } else {
                    String permStr = String.join("/", perms);
                    report.append(String.format(" %-14s │", truncate(permStr, 14)));
                }
            }
            report.append("\n");
        }

        report.append("+").append(repeatString(LINE, 20)).append("+");
        for (int i = 0; i < resources.size(); i++) {
            report.append(repeatString(LINE, 15));
            if (i < resources.size() - 1) {
                report.append("+");
            }
        }
        report.append("+\n\n");

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

    public void exportToFile(String report, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
            System.out.println("Отчёт сохранён в файл: " + filename);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении отчёта: " + e.getMessage());
        }
    }
}
