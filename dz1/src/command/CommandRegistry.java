package command;

import filters.*;
import interfaces.RoleAssignment;
import record.Permission;
import record.User;
import role.PermanentAssignment;
import role.Role;
import record.AssignmentMetadata;
import role.TemporaryAssignment;
import util.*;
import util.AuditActions;
import util.AuditEntry;
import util.ReportGenerator;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String DATA_FILE = "rbac_data.txt";

    public static void registerAllCommands(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerUtilityCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        // user-list - список всех пользователей
        parser.registerCommand("user-list", "Вывести список всех пользователей", ((scanner, system) -> {
            ConsoleUtils.printHeader("СПИСОК ПОЛЬЗОВАТЕЛЕЙ");

            List<User> users = system.getUserManager().findAll();

            if (users.isEmpty()) {
                ConsoleUtils.printInfo("В системе нет пользователей");
                return;
            }

            System.out.println(FormatUtils.formatUsersTable(users));
        }));

        // user-create - создать нового пользователя
        parser.registerCommand("user-create", "Создание нового пользователя", ((scanner, system) -> {
            ConsoleUtils.printHeader("СОЗДАНИЕ НОВОГО ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите имя пользователя", true);
            String fullName = ConsoleUtils.promptString(scanner, "Введите полное имя", true);
            String email = ConsoleUtils.promptString(scanner, "Введите email", true);

            if (!ValidationUtils.isValidEmail(email)) {
                ConsoleUtils.printError("Неверный формат email");
                return;
            }

            try {
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                system.getAuditLog().log(
                        AuditActions.CREATE_USER,
                        system.getCurrentUser(),
                        username,
                        String.format("Создан пользователь: %s, email: %s", fullName, email)
                );
                ConsoleUtils.printSuccess("Пользователь успешно создан: " + user.format());
            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError("Ошибка валидации: " + e.getMessage());
            } catch (IllegalStateException e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        }));

        // user-view - просмотр информации о пользователе
        parser.registerCommand("user-view", "Просмотр информации о пользователе", ((scanner, system) -> {
            ConsoleUtils.printHeader("ПРОСМОТР ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите имя пользователя", true);

            system.getUserManager().findByUsername(username).ifPresentOrElse(
                    user -> printUserDetailedInfo(user, system),
                    () -> ConsoleUtils.printError("Пользователь " + username + " не найден"));
        }));

        // user-update - обновить данные пользователя
        parser.registerCommand("user-update", "Обновить данные пользователя", ((scanner, system) -> {
            ConsoleUtils.printHeader("ОБНОВЛЕНИЕ ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите имя пользователя", true);

            if (!system.getUserManager().exists(username)) {
                ConsoleUtils.printError("Пользователь " + username + " не найден");
                return;
            }

            String newFullname = ConsoleUtils.promptString(scanner, "Введите новое полное имя (Enter - без изменений)", false);
            String newEmail = ConsoleUtils.promptString(scanner, "Введите новый email (Enter - без изменений)", false);

            User currentUser = system.getUserManager().findByUsername(username).get();

            if (newFullname.isEmpty()) {
                newFullname = currentUser.fullname();
            }

            if (newEmail.isEmpty()) {
                newEmail = currentUser.email();
            }

            try {
                system.getUserManager().update(username, newFullname, newEmail);
                ConsoleUtils.printSuccess("Данные пользователя " + username + " обновлены");
            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        }));

        // user-delete - удалить пользователя
        parser.registerCommand("user-delete", "Удалить пользователя", ((scanner, system) -> {
            ConsoleUtils.printHeader("УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите имя пользователя", true);

            User user = system.getUserManager().findByUsername(username).orElse(null);

            if (user == null) {
                ConsoleUtils.printError("Пользователь " + username + " не найден");
                return;
            }

            if (user.username().equals(system.getCurrentUser())) {
                ConsoleUtils.printError("Невозможно удалить текущего пользователя системы");
                return;
            }

            if (!ConsoleUtils.promptYesNo(scanner, "Вы уверены, что хотите удалить пользователя?")) {
                ConsoleUtils.printInfo("Операция отменена");
                return;
            }

            List<RoleAssignment> userAssignment = system.getAssignmentManager().findByUser(user);
            int assignmentCount = userAssignment.size();

            for (RoleAssignment assignment : userAssignment) {
                system.getAssignmentManager().remove(assignment);
            }

            boolean removed = system.getUserManager().remove(user);

            if (removed) {
                system.getAuditLog().log(
                        AuditActions.DELETE_USER,
                        system.getCurrentUser(),
                        username,
                        String.format("Удален пользователь. Удалено назначений: %d", assignmentCount)
                );

                ConsoleUtils.printSuccess("Пользователь " + username + " удален");
                ConsoleUtils.printInfo("Удалено назначений: " + userAssignment.size());
            } else {
                ConsoleUtils.printError("Ошибка при удалении пользователя");
            }
        }));

        // user-search - поиск пользователей по фильтрам
        parser.registerCommand("user-search", "Поиск пользователей по фильтрам", ((scanner, system) -> {
            ConsoleUtils.printHeader("ПОИСК ПОЛЬЗОВАТЕЛЕЙ");

            ConsoleUtils.printSubheader("Выберите тип фильтра");
            System.out.println("1 - По username (содержит)");
            System.out.println("2 - По email (содержит)");
            System.out.println("3 - По домену email");
            System.out.println("4 - По полному имени (содержит)");
            System.out.println("0 - Отмена");

            int choice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 0, 4);

            UserFilter filter = null;
            String filterDescription = "";

            switch (choice) {
                case 1:
                    String usernameSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в username", true);
                    filter = UserFilters.byUsernameContains(usernameSub);
                    filterDescription = "username содержит '" + usernameSub + "'";
                    break;

                case 2:
                    String emailSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в email", true);
                    filter = UserFilters.byEmail(emailSub);
                    filterDescription = "email содержит '" + emailSub + "'";
                    break;

                case 3:
                    String domain = ConsoleUtils.promptString(scanner, "Введите домен email (например, @gmail.com)", true);
                    filter = UserFilters.byEmailDomain(domain);
                    filterDescription = "email домен '" + domain + "'";
                    break;

                case 4:
                    String nameSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в полном имени", true);
                    filter = UserFilters.byFullNameContains(nameSub);
                    filterDescription = "полное имя содержит '" + nameSub + "'";
                    break;

                case 0:
                    ConsoleUtils.printInfo("Поиск отменен");
                    return;
            }

            // Выполняем поиск
            List<User> results = system.getUserManager().findByFilter(filter);

            ConsoleUtils.printSubheader("РЕЗУЛЬТАТЫ ПОИСКА");
            ConsoleUtils.printInfo("Фильтр: " + filterDescription);
            ConsoleUtils.printInfo("Найдено пользователей: " + results.size());

            if (!results.isEmpty()) {
                System.out.println(FormatUtils.formatUsersTable(results));
            }
        }));
    }

    private static void registerRoleCommands(CommandParser parser) {

        // role-list — вывести список всех ролей
        parser.registerCommand("role-list", "Вывести список всех ролей", (scanner, system) -> {
            ConsoleUtils.printHeader("СПИСОК ВСЕХ РОЛЕЙ");

            List<Role> roles = system.getRoleManager().findAll();

            if (roles.isEmpty()) {
                ConsoleUtils.printInfo("В системе нет ролей");
                return;
            }

            System.out.println(FormatUtils.formatRolesTable(roles));
        });

        // role-create — создать новую роль
        parser.registerCommand("role-create", "Создать новую роль", ((scanner, system) -> {
            ConsoleUtils.printHeader("СОЗДАНИЕ НОВОЙ РОЛИ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите название роли", true);
            String description = ConsoleUtils.promptString(scanner, "Введите описание роли", true);

            try {
                Role role = new Role(roleName, description);
                system.getRoleManager().add(role);

                system.getAuditLog().log(
                        AuditActions.CREATE_ROLE,
                        system.getCurrentUser(),
                        roleName,
                        "Создана новая роль: " + description
                );

                ConsoleUtils.printSuccess("Новая роль " + roleName + " создана");

                addPermissionsToRole(role, scanner, system);
            } catch (IllegalStateException e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        }));

        // role-view — просмотр роли
        parser.registerCommand("role-view", "Просмотр информации о роли", (scanner, system) -> {
            ConsoleUtils.printHeader("ПРОСМОТР ИНФОРМАЦИИ О РОЛИ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите название роли", true);

            system.getRoleManager().findByName(roleName).ifPresentOrElse(
                    role -> printRoleDetailedInfo(role, system),
                    () -> ConsoleUtils.printError("Роль " + roleName + " не найдена")
            );
        });

        // role-update — обновить роль
        parser.registerCommand("role-update", "Обновить название и описание роли", (scanner, system) -> {
            ConsoleUtils.printHeader("ОБНОВЛЕНИЕ ДАННЫХ РОЛИ");

            String oldName = ConsoleUtils.promptString(scanner, "Введите название роли для обновления", true);

            Role role = system.getRoleManager().findByName(oldName).orElse(null);

            if (role == null) {
                ConsoleUtils.printError("Роль " + oldName + " не найдена");
                return;
            }

            String newName = ConsoleUtils.promptString(scanner, "Введите новое название роли (Enter - без изменений)", false);
            if (newName.isEmpty()) {
                newName = oldName;
            }

            String newDescription = ConsoleUtils.promptString(scanner, "Введите новое описание роли (Enter - без изменений)", false);
            if (newDescription.isEmpty()) {
                newDescription = role.getDescription();
            }

            try {
                system.getRoleManager().remove(role);

                Role updateRole = new Role(newName, newDescription);

                for (Permission p : role.getPermissions()) {
                    updateRole.addPermissions(p);
                }
                system.getRoleManager().add(updateRole);
                ConsoleUtils.printSuccess("Роль обновлена");
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
                ConsoleUtils.printWarning("Попытка восстановить роль");
                try {
                    system.getRoleManager().add(role);
                    ConsoleUtils.printSuccess("Роль восстановлена");
                } catch (Exception ex) {
                    ConsoleUtils.printError("Критическая ошибка: " + ex.getMessage());
                }
            }
        });

        // role-delete — удалить роль
        parser.registerCommand("role-delete", "Удаление роли", ((scanner, system) -> {
            ConsoleUtils.printHeader("УДАЛЕНИЕ РОЛИ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите название роли", true);

            Role role = system.getRoleManager().findByName(roleName).orElse(null);

            if (role == null) {
                ConsoleUtils.printError("Роль " + roleName + " не найдена");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
            List<RoleAssignment> activeAssignments = assignments.stream().filter(RoleAssignment::isActive).collect(Collectors.toList());

            if (!activeAssignments.isEmpty()) {
                ConsoleUtils.printWarning("Роль назначена пользователям!");
                ConsoleUtils.printInfo("Активных назначений: " + activeAssignments.size());

                for (RoleAssignment ra : activeAssignments) {
                    System.out.printf("  - %s (%s)\n",
                            ra.user().username(),
                            ra.assignmentType());
                }

                if (!ConsoleUtils.promptYesNo(scanner, "Введите 'да' для принудительного удаления (назначения будут отозваны)")) {
                    ConsoleUtils.printInfo("Операция отменена");
                    return;
                }

                for (RoleAssignment ra : activeAssignments) {
                    if (ra instanceof PermanentAssignment) {
                        ((PermanentAssignment) ra).revoke();
                    }
                }
            }

            try {
                boolean removed = system.getRoleManager().remove(role);
                if (removed) {
                    ConsoleUtils.printSuccess("Роль " + roleName + " удалена");
                } else {
                    ConsoleUtils.printError("Ошибка при удалении роли");
                }
            } catch (IllegalStateException e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        }));

        // role-add-permission — добавить право к роли
        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, system) -> {
            ConsoleUtils.printHeader("ДОБАВЛЕНИЕ ПРАВА К РОЛИ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите название роли", true);

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                ConsoleUtils.printError("Роль " + roleName + " не найдена");
                return;
            }

            ConsoleUtils.printSubheader("Введите данные права");
            String permName = ConsoleUtils.promptString(scanner, "Название права (READ, WRITE, DELETE и т.д.)", true).toUpperCase();
            String resource = ConsoleUtils.promptString(scanner, "Ресурс (users, roles, reports и т.д.)", true).toLowerCase();
            String permDescription = ConsoleUtils.promptString(scanner, "Описание", true);

            try {
                Permission permission = new Permission(permName, resource, permDescription);
                system.getRoleManager().addPermissionToRole(roleName, permission);
                ConsoleUtils.printSuccess("Право добавлено к роли '" + roleName + "'");
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        });

        // role-remove-permission — удалить право из роли
        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, system) -> {
            ConsoleUtils.printHeader("УДАЛЕНИЕ ПРАВА ИЗ РОЛИ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите название роли", true);

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                ConsoleUtils.printError("Роль " + roleName + " не найдена");
                return;
            }

            Set<Permission> permissions = role.getPermissions();
            if (permissions.isEmpty()) {
                ConsoleUtils.printInfo("У роли нет прав для удаления");
                return;
            }

            ConsoleUtils.printSubheader("Права роли " + roleName);
            List<Permission> permList = new ArrayList<>(permissions);
            for (int i = 0; i < permList.size(); i++) {
                Permission p = permList.get(i);
                System.out.printf("  %d. %s on %s: %s\n",
                        i + 1, p.name(), p.resource(), p.description());
            }

            int choice = ConsoleUtils.promptInt(scanner, "Введите номер права для удаления (0 - отмена)", 0, permList.size());

            if (choice == 0) {
                ConsoleUtils.printInfo("Операция отменена");
                return;
            }

            int index = choice - 1;
            try {
                Permission toRemove = permList.get(index);
                system.getRoleManager().removePermissionFromRole(roleName, toRemove);
                ConsoleUtils.printSuccess("Право удалено");
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        });

        // role-search — поиск ролей
        parser.registerCommand("role-search", "Поиск ролей по фильтрам", (scanner, system) -> {
            ConsoleUtils.printHeader("ПОИСК РОЛЕЙ");

            ConsoleUtils.printSubheader("Выберите тип фильтра");
            System.out.println("1 - По названию (содержит)");
            System.out.println("2 - По наличию конкретного права");
            System.out.println("3 - По минимальному количеству прав");
            System.out.println("0 - Отмена");

            int choice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 0, 3);

            RoleFilter filter = null;
            String filterDescription = "";

            switch (choice) {
                case 1:
                    String nameSub = ConsoleUtils.promptString(scanner, "Введите подстроку для поиска в названии", true);
                    filter = RoleFilters.byNameContains(nameSub);
                    filterDescription = "название содержит '" + nameSub + "'";
                    break;

                case 2:
                    String permName = ConsoleUtils.promptString(scanner, "Введите название права (READ, WRITE и т.д.)", true).toUpperCase();
                    String resource = ConsoleUtils.promptString(scanner, "Введите ресурс", true).toLowerCase();

                    filter = RoleFilters.hasPermission(permName, resource);
                    filterDescription = "имеет право " + permName + " на " + resource;
                    break;

                case 3:
                    int minCount = ConsoleUtils.promptInt(scanner, "Введите минимальное количество прав", 0, 100);
                    filter = RoleFilters.hasAtLeastNPermissions(minCount);
                    filterDescription = "минимум " + minCount + " прав";
                    break;

                case 0:
                    ConsoleUtils.printInfo("Поиск отменен");
                    return;
            }

            // Выполняем поиск
            List<Role> results = system.getRoleManager().findByFilter(filter);

            ConsoleUtils.printSubheader("РЕЗУЛЬТАТЫ ПОИСКА");
            ConsoleUtils.printInfo("Фильтр: " + filterDescription);
            ConsoleUtils.printInfo("Найдено ролей: " + results.size());

            if (!results.isEmpty()) {
                System.out.println(FormatUtils.formatRolesTable(results));
            }
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {

        // assign-role — назначить роль пользователю
        parser.registerCommand("assign-role", "Назначить роль пользователю", (scanner, system) -> {
            ConsoleUtils.printHeader("НАЗНАЧЕНИЕ РОЛИ ПОЛЬЗОВАТЕЛЮ");

            String username = ConsoleUtils.promptString(scanner, "Введите username пользователя", true);

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                ConsoleUtils.printError("Пользователь " + username + " не найден");
                return;
            }

            List<Role> availableRoles = system.getRoleManager().findAll();
            if (availableRoles.isEmpty()) {
                ConsoleUtils.printError("В системе нет доступных ролей");
                return;
            }

            // Используем promptChoice для выбора роли
            Role selectedRole = ConsoleUtils.promptChoice(scanner, "Выберите роль", availableRoles);
            if (selectedRole == null) {
                ConsoleUtils.printInfo("Операция отменена");
                return;
            }

            if (system.getAssignmentManager().hasActiveAssignment(username, selectedRole.getName())) {
                ConsoleUtils.printError("У пользователя уже есть активное назначение на эту роль");
                return;
            }

            ConsoleUtils.printSubheader("Выберите тип назначения");
            System.out.println("1 - Постоянное");
            System.out.println("2 - Временное");

            int typeChoice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 1, 2);

            String reason = ConsoleUtils.promptString(scanner, "Введите причину назначения (Enter - пропустить)", false);
            if (reason.isEmpty()) {
                reason = "Назначено через консоль";
            }

            AssignmentMetadata metadata = new AssignmentMetadata(
                    system.getCurrentUser(),
                    LocalDateTime.now().format(FORMATTER),
                    reason
            );

            RoleAssignment assignment = null;
            String assignmentType = "";

            switch (typeChoice) {
                case 1: // Постоянное
                    assignment = new PermanentAssignment(user, selectedRole, metadata);
                    assignmentType = "постоянное";
                    break;

                case 2: // Временное
                    String expiryDate = ConsoleUtils.promptString(scanner, "Введите дату истечения (формат: yyyy-MM-dd HH:mm)", true);

                    try {
                        LocalDateTime.parse(expiryDate, FORMATTER);

                        // Проверяем, не в прошлом ли дата
                        if (DateUtils.isPast(expiryDate.substring(0, 10))) {
                            ConsoleUtils.printWarning("Дата в прошлом! (" + DateUtils.formatRelativeTime(expiryDate.substring(0, 10)) + ")");
                            if (!ConsoleUtils.promptYesNo(scanner, "Продолжить?")) {
                                return;
                            }
                        } else {
                            ConsoleUtils.printInfo("Истекает: " + DateUtils.formatRelativeTime(expiryDate.substring(0, 10)));
                        }

                        assignment = new TemporaryAssignment(
                                user, selectedRole, metadata, expiryDate, false
                        );
                        assignmentType = "временное (до " + expiryDate + ")";
                    } catch (DateTimeParseException e) {
                        ConsoleUtils.printError("Неверный формат даты");
                        return;
                    }
                    break;
            }

            system.getAssignmentManager().add(assignment);

            system.getAuditLog().log(
                    AuditActions.ASSIGN_ROLE,
                    system.getCurrentUser(),
                    username,
                    String.format("Назначена роль '%s' (%s). Причина: %s",
                            selectedRole.getName(), assignmentType, reason)
            );

            ConsoleUtils.printSuccess("Роль успешно назначена!");
            ConsoleUtils.printInfo("ID назначения: " + assignment.assignmentId());
        });

        // revoke-role — отозвать роль у пользователя
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, system) -> {
            ConsoleUtils.printHeader("ОТЗЫВ РОЛИ У ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите username пользователя", true);

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                ConsoleUtils.printError("Пользователь '" + username + "' не найден");
                return;
            }

            List<RoleAssignment> activeAssignments = system.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (activeAssignments.isEmpty()) {
                ConsoleUtils.printInfo("У пользователя нет активных назначений");
                return;
            }

            ConsoleUtils.printSubheader("Активные назначения пользователя");
            for (int i = 0; i < activeAssignments.size(); i++) {
                RoleAssignment ra = activeAssignments.get(i);
                String typeInfo = ra.assignmentType();
                if (ra instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) ra;
                    typeInfo += " (до " + temp.getExpiresAt() + ")";
                }
                System.out.printf("  %d. %s [%s] - %s\n",
                        i + 1, ra.role().getName(), typeInfo, ra.assignmentId());
            }

            int choice = ConsoleUtils.promptInt(scanner, "Выберите номер назначения для отзыва (0 - отмена)", 0, activeAssignments.size());

            if (choice == 0) {
                ConsoleUtils.printInfo("Операция отменена");
                return;
            }

            int index = choice - 1;
            try {
                RoleAssignment toRevoke = activeAssignments.get(index);

                if (toRevoke instanceof PermanentAssignment) {
                    system.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
                    ConsoleUtils.printSuccess("Назначение отозвано");
                } else {
                    ConsoleUtils.printError("Отмена временных назначений пока не поддерживается");
                }
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        });

        // assignment-list — список всех назначений
        parser.registerCommand("assignment-list", "Список всех назначений", (scanner, system) -> {
            ConsoleUtils.printHeader("СПИСОК ВСЕХ НАЗНАЧЕНИЙ");

            List<RoleAssignment> assignments = system.getAssignmentManager().findAll();

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("В системе нет назначений");
                return;
            }

            System.out.println(FormatUtils.formatAssignmentsTable(assignments));
        });

        // assignment-list-user — назначения конкретного пользователя
        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя", (scanner, system) -> {
            ConsoleUtils.printHeader("НАЗНАЧЕНИЯ ПОЛЬЗОВАТЕЛЯ");

            String username = ConsoleUtils.promptString(scanner, "Введите username", true);

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                ConsoleUtils.printError("Пользователь '" + username + "' не найден");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("У пользователя нет назначений");
                return;
            }

            ConsoleUtils.printSubheader("Назначения пользователя " + username);
            for (RoleAssignment ra : assignments) {
                printAssignmentDetails(ra);
            }
        });

        // assignment-list-role — список пользователей с конкретной ролью
        parser.registerCommand("assignment-list-role", "Список пользователей с ролью", (scanner, system) -> {
            ConsoleUtils.printHeader("ПОЛЬЗОВАТЕЛИ С РОЛЬЮ");

            String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                ConsoleUtils.printError("Роль '" + roleName + "' не найдена");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);

            if (assignments.isEmpty()) {
                ConsoleUtils.printInfo("Нет пользователей с ролью '" + roleName + "'");
                return;
            }

            ConsoleUtils.printSubheader("Пользователи с ролью " + roleName);
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s] - %s\n",
                        ra.user().username(),
                        ra.assignmentType(),
                        status);
            }
        });

        // assignment-active — только активные назначения
        parser.registerCommand("assignment-active", "Только активные назначения", (scanner, system) -> {
            ConsoleUtils.printHeader("АКТИВНЫЕ НАЗНАЧЕНИЯ");

            List<RoleAssignment> active = system.getAssignmentManager().getActiveAssignments();

            if (active.isEmpty()) {
                ConsoleUtils.printInfo("Нет активных назначений");
                return;
            }

            System.out.println(FormatUtils.formatAssignmentsTable(active));
        });

        // assignment-expired — истёкшие временные назначения
        parser.registerCommand("assignment-expired", "Истекшие временные назначения", (scanner, system) -> {
            ConsoleUtils.printHeader("ИСТЕКШИЕ ВРЕМЕННЫЕ НАЗНАЧЕНИЯ");

            List<RoleAssignment> expired = system.getAssignmentManager().getExpiredAssignments().stream()
                    .filter(ra -> ra instanceof TemporaryAssignment)
                    .collect(Collectors.toList());

            if (expired.isEmpty()) {
                ConsoleUtils.printInfo("Нет истекших назначений");
                return;
            }

            System.out.println(FormatUtils.formatAssignmentsTable(expired));
        });

        // assignment-extend — продлить временное назначение
        parser.registerCommand("assignment-extend", "Продлить временное назначение", (scanner, system) -> {
            ConsoleUtils.printHeader("ПРОДЛЕНИЕ НАЗНАЧЕНИЯ");

            System.out.println("Выберите способ поиска назначения:");
            System.out.println("  1 - По ID назначения");
            System.out.println("  2 - По пользователю + роли");

            int choice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 1, 2);
            String assignmentId = null;

            try {
                switch (choice) {
                    case 1:
                        assignmentId = ConsoleUtils.promptString(scanner, "Введите ID назначения", true);
                        break;

                    case 2:
                        String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                        String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);

                        List<RoleAssignment> assignments = system.getAssignmentManager().findByUsername(username).stream()
                                .filter(ra -> ra.role().getName().equals(roleName))
                                .filter(RoleAssignment::isActive)
                                .collect(Collectors.toList());

                        if (assignments.isEmpty()) {
                            ConsoleUtils.printError("Активное назначение не найдено");
                            return;
                        }

                        if (assignments.size() > 1) {
                            ConsoleUtils.printError("Найдено несколько назначений. Используйте поиск по ID.");
                            return;
                        }

                        assignmentId = assignments.get(0).assignmentId();
                        break;
                }

                RoleAssignment assignment = system.getAssignmentManager().findById(assignmentId).orElse(null);
                if (assignment == null) {
                    ConsoleUtils.printError("Назначение не найдено");
                    return;
                }

                if (!(assignment instanceof TemporaryAssignment)) {
                    ConsoleUtils.printError("Можно продлевать только временные назначения");
                    return;
                }

                String newDate = ConsoleUtils.promptString(scanner, "Введите новую дату истечения (формат: yyyy-MM-dd HH:mm)", true);

                system.getAssignmentManager().extendTemporaryAssignment(assignmentId, newDate);
                ConsoleUtils.printSuccess("Назначение продлено до " + newDate);

            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        });

        // assignment-search — поиск назначений по фильтрам
        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам", (scanner, system) -> {
            ConsoleUtils.printHeader("ПОИСК НАЗНАЧЕНИЙ");

            ConsoleUtils.printSubheader("Выберите тип фильтра");
            System.out.println("1 - По пользователю");
            System.out.println("2 - По роли");
            System.out.println("3 - По типу (постоянное/временное)");
            System.out.println("4 - По статусу (активное/неактивное)");
            System.out.println("5 - Назначенные после даты");
            System.out.println("6 - Истекающие до даты");
            System.out.println("0 - Отмена");

            int filterChoice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 0, 6);

            AssignmentFilter filter = null;
            String filterDescription = "";

            try {
                switch (filterChoice) {
                    case 1: // По пользователю
                        String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                        filter = AssignmentFilters.byUsername(username);
                        filterDescription = "пользователь = " + username;
                        break;

                    case 2: // По роли
                        String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
                        filter = AssignmentFilters.byRoleName(roleName);
                        filterDescription = "роль = " + roleName;
                        break;

                    case 3: // По типу
                        System.out.println("1 - PERMANENT (постоянные)");
                        System.out.println("2 - TEMPORARY (временные)");
                        int typeChoice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 1, 2);

                        if (typeChoice == 1) {
                            filter = AssignmentFilters.byType(RoleAssignment.TYPE_PERMANENT);
                            filterDescription = "тип = PERMANENT";
                        } else {
                            filter = AssignmentFilters.byType(RoleAssignment.TYPE_TEMPORARY);
                            filterDescription = "тип = TEMPORARY";
                        }
                        break;

                    case 4: // По статусу
                        System.out.println("1 - ACTIVE (активные)");
                        System.out.println("2 - INACTIVE (неактивные)");
                        int statusChoice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 1, 2);

                        if (statusChoice == 1) {
                            filter = AssignmentFilters.activeOnly();
                            filterDescription = "статус = ACTIVE";
                        } else {
                            filter = AssignmentFilters.inactiveOnly();
                            filterDescription = "статус = INACTIVE";
                        }
                        break;

                    case 5: // Назначенные после даты
                        String afterDate = ConsoleUtils.promptString(scanner, "Введите дату (формат: yyyy-MM-dd HH:mm)", true);
                        filter = AssignmentFilters.assignedAfter(afterDate);
                        filterDescription = "назначено после " + afterDate;
                        break;

                    case 6: // Истекающие до даты
                        String beforeDate = ConsoleUtils.promptString(scanner, "Введите дату (формат: yyyy-MM-dd HH:mm)", true);
                        filter = AssignmentFilters.expiringBefore(beforeDate);
                        filterDescription = "истекает до " + beforeDate;
                        break;

                    case 0:
                        ConsoleUtils.printInfo("Поиск отменен");
                        return;
                }

                List<RoleAssignment> results = system.getAssignmentManager().findByFilter(filter);

                ConsoleUtils.printSubheader("РЕЗУЛЬТАТЫ ПОИСКА");
                ConsoleUtils.printInfo("Фильтр: " + filterDescription);
                ConsoleUtils.printInfo("Найдено назначений: " + results.size());

                if (!results.isEmpty()) {
                    System.out.println(FormatUtils.formatAssignmentsTable(results));
                }

            } catch (DateTimeParseException e) {
                ConsoleUtils.printError("Неверный формат даты");
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        });
    }

    private static void registerUtilityCommands(CommandParser parser) {

        parser.registerCommand("report-users-async", "Асинхронный отчёт по пользователям", (scanner, system) -> {
            ConsoleUtils.printHeader("АСИНХРОННЫЙ ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ");

            System.out.println("Генерация отчёта запущена в фоновом режиме...");

            // Запускаем в отдельном потоке
            Thread thread = new Thread(() -> {
                ReportGenerator reportGen = new ReportGenerator();
                String report = reportGen.generateUserReportParallel(
                        system.getUserManager(),
                        system.getAssignmentManager()
                );

                System.out.println(report);

                if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                    String filename = "user_report_async_" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
                    reportGen.exportToFile(report, filename);
                }
            });

            thread.start();
            System.out.println("✅ Отчёт генерируется в фоновом режиме. Вы можете продолжать работу.");
        });

        parser.registerCommand("save-async", "Асинхронное сохранение данных", (scanner, system) -> {
            ConsoleUtils.printHeader("АСИНХРОННОЕ СОХРАНЕНИЕ ДАННЫХ");

            System.out.println("Сохранение данных запущено в фоновом режиме...");

            Thread thread = new Thread(() -> {
                // Вызываем существующую команду save
                Command saveCommand = parser.getCommand("save");
                if (saveCommand != null) {
                    saveCommand.execute(scanner, system);
                }
            });

            thread.start();
            System.out.println("✅ Данные сохраняются в фоновом режиме. Вы можете продолжать работу.");
        });

        // report-users — отчёт по пользователям
        parser.registerCommand("report-users", "Отчёт по пользователям", (scanner, system) -> {
            ConsoleUtils.printHeader("ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ");

            System.out.println("Выберите режим:");
            System.out.println("  1 - Синхронный (обычный)");
            System.out.println("  2 - Асинхронный (фоновый)");

            int choice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 1, 2);

            if (choice == 2) {
                // Вызываем асинхронную версию
                parser.executeCommand("report-users-async", scanner, system);
            } else {
                // Синхронная версия
                ReportGenerator reportGen = new ReportGenerator();
                String report = reportGen.generateUserReportParallel(
                        system.getUserManager(),
                        system.getAssignmentManager()
                );
                System.out.println(report);

                if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                    String filename = "user_report_" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
                    reportGen.exportToFile(report, filename);
                }
            }
        });

        // report-roles — отчёт по ролям
        parser.registerCommand("report-roles", "Отчёт по ролям с количеством пользователей", ((scanner, system) -> {
            ConsoleUtils.printHeader("ГЕНЕРАЦИЯ ОТЧЕТА ПО РОЛЯМ");

            ReportGenerator reportGen = new ReportGenerator();
            String report = reportGen.generateRoleReportParallel(system.getRoleManager(), system.getAssignmentManager());

            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Введите имя файла (Enter для role_report.txt)", false);
                if (filename.isEmpty()) {
                    filename = "role_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
                }

                reportGen.exportToFile(report, filename);

                system.getAuditLog().log(
                        "REPORT_EXPORT",
                        system.getCurrentUser(),
                        "roles",
                        "Экспортирован отчёт по ролям в файл: " + filename
                );
            }
        }));

        parser.registerCommand("report-matrix", "Матрица прав доступа (пользователи × ресурсы)", (scanner, system) -> {
            ConsoleUtils.printHeader("ГЕНЕРАЦИЯ МАТРИЦЫ ПРАВ");

            ReportGenerator reportGen = new ReportGenerator();
            String report = reportGen.generatePermissionMatrixParallel(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                String filename = ConsoleUtils.promptString(scanner, "Введите имя файла (Enter для matrix_report.txt)", false);
                if (filename.isEmpty()) {
                    filename = "matrix_report_" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
                }

                reportGen.exportToFile(report, filename);

                system.getAuditLog().log(
                        "REPORT_EXPORT",
                        system.getCurrentUser(),
                        "matrix",
                        "Экспортирована матрица прав в файл: " + filename
                );
            }
        });

        // help — справка по командам
        parser.registerCommand("help", "Показать список всех команд", (scanner, system) -> {
            parser.printHelp();
        });

        // audit-log — просмотр журнала аудита
        parser.registerCommand("audit-log", "Просмотр журнала аудита", (scanner, system) -> {
            ConsoleUtils.printHeader("ЖУРНАЛ АУДИТА");

            ConsoleUtils.printSubheader("Выберите действие");
            System.out.println("1 - Показать все записи");
            System.out.println("2 - Показать записи по исполнителю");
            System.out.println("3 - Показать записи по действию");
            System.out.println("4 - Сохранить лог в файл");
            System.out.println("5 - Очистить лог");
            System.out.println("0 - Назад");

            int choice = ConsoleUtils.promptInt(scanner, "Ваш выбор", 0, 5);

            switch (choice) {
                case 1:
                    system.getAuditLog().printLog();
                    break;

                case 2:
                    String performer = ConsoleUtils.promptString(scanner, "Введите имя исполнителя", true);
                    List<AuditEntry> byPerformer = system.getAuditLog().getByPerformer(performer);
                    if (byPerformer.isEmpty()) {
                        ConsoleUtils.printInfo("Записей для исполнителя '" + performer + "' не найдено");
                    } else {
                        ConsoleUtils.printSubheader("ЗАПИСИ ИСПОЛНИТЕЛЯ " + performer);
                        for (AuditEntry entry : byPerformer) {
                            System.out.println("  " + entry.format());
                        }
                        ConsoleUtils.printInfo("Всего: " + byPerformer.size());
                    }
                    break;

                case 3:
                    String action = ConsoleUtils.promptString(scanner, "Введите действие (CREATE_USER, DELETE_ROLE и т.д.)", true).toUpperCase();
                    List<AuditEntry> byAction = system.getAuditLog().getByAction(action);
                    if (byAction.isEmpty()) {
                        ConsoleUtils.printInfo("Записей для действия '" + action + "' не найдено");
                    } else {
                        ConsoleUtils.printSubheader("ЗАПИСИ ДЕЙСТВИЯ " + action);
                        for (AuditEntry entry : byAction) {
                            System.out.println("  " + entry.format());
                        }
                        ConsoleUtils.printInfo("Всего: " + byAction.size());
                    }
                    break;

                case 4:
                    String filename = ConsoleUtils.promptString(scanner, "Введите имя файла (например, audit.log)", true);
                    system.getAuditLog().saveToFile(filename);
                    break;

                case 5:
                    if (ConsoleUtils.promptYesNo(scanner, "Очистить лог?")) {
                        system.getAuditLog().clear();
                    }
                    break;

                case 0:
                    return;
            }
        });

        // stats — статистика системы
        parser.registerCommand("stats", "Показать расширенную статистику системы", (scanner, system) -> {
            ConsoleUtils.printHeader("СТАТИСТИКА СИСТЕМЫ");

            // Базовая статистика
            System.out.println(system.generateStatistics());

            // Дополнительная статистика
            int userCount = system.getUserManager().count();
            int roleCount = system.getRoleManager().count();
            int assignmentCount = system.getAssignmentManager().count();
            int activeCount = system.getAssignmentManager().getActiveAssignments().size();
            int expiredCount = system.getAssignmentManager().getExpiredAssignments().size();

            // Среднее количество ролей на пользователя
            double avgRolesPerUser = 0;
            if (userCount > 0) {
                int totalAssignments = assignmentCount;
                avgRolesPerUser = (double) totalAssignments / userCount;
            }

            ConsoleUtils.printInfo(String.format("Среднее количество ролей на пользователя: %.2f", avgRolesPerUser));

            // Топ-3 самых популярных ролей
            ConsoleUtils.printSubheader("Топ-3 самых популярных ролей");

            Map<String, Integer> rolePopularity = new HashMap<>();
            for (RoleAssignment ra : system.getAssignmentManager().findAll()) {
                String roleName = ra.role().getName();
                rolePopularity.put(roleName, rolePopularity.getOrDefault(roleName, 0) + 1);
            }

            if (rolePopularity.isEmpty()) {
                ConsoleUtils.printInfo("(нет данных)");
            } else {
                rolePopularity.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(3)
                        .forEach(entry -> System.out.printf("  %d. %s (%d назначений)\n",
                                new ArrayList<>(rolePopularity.keySet()).indexOf(entry.getKey()) + 1,
                                entry.getKey(), entry.getValue()));
            }
        });

        // clear — очистить экран
        parser.registerCommand("clear", "Очистить экран", (scanner, system) -> {
            ConsoleUtils.clearScreen();
        });

        // exit — выход из программы
        parser.registerCommand("exit", "Выйти из программы", (scanner, system) -> {
            ConsoleUtils.printHeader("ВЫХОД ИЗ ПРОГРАММЫ");

            if (ConsoleUtils.promptYesNo(scanner, "Сохранить данные перед выходом?")) {
                parser.executeCommand("save", scanner, system);
            }

            if (ConsoleUtils.promptYesNo(scanner, "Вы уверены, что хотите выйти?")) {
                system.getAuditLog().log(
                        AuditActions.SYSTEM_STOP,
                        system.getCurrentUser(),
                        "system",
                        "Завершение работы системы"
                );

                ConsoleUtils.printSuccess("До свидания!");
                System.exit(0);
            } else {
                ConsoleUtils.printInfo("Выход отменен");
            }
        });

        // save — сохранить данные в файл
        parser.registerCommand("save", "Сохранить данные в файл", (scanner, system) -> {
            ConsoleUtils.printHeader("СОХРАНЕНИЕ ДАННЫХ");

            String filename = DATA_FILE;
            File file = new File(filename);

            if (file.exists()) {
                if (!ConsoleUtils.promptYesNo(scanner, "Файл уже существует. Перезаписать?")) {
                    ConsoleUtils.printInfo("Сохранение отменено");
                    return;
                }
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {

                writer.println("# USERS");
                List<User> users = system.getUserManager().findAll();
                for (User user : users) {
                    writer.printf("USER|%s|%s|%s\n",
                            user.username(), user.fullname(), user.email());
                }
                writer.println();

                writer.println("# ROLES");
                List<Role> roles = system.getRoleManager().findAll();
                for (Role role : roles) {
                    StringBuilder permsBuilder = new StringBuilder();
                    for (Permission p : role.getPermissions()) {
                        permsBuilder.append(p.name()).append(":")
                                .append(p.resource()).append(":")
                                .append(p.description()).append(";");
                    }
                    writer.printf("ROLE|%s|%s|%s|%s\n",
                            role.getId(), role.getName(), role.getDescription(),
                            permsBuilder.toString());
                }
                writer.println();

                writer.println("# ASSIGNMENTS");
                List<RoleAssignment> assignments = system.getAssignmentManager().findAll();
                for (RoleAssignment ra : assignments) {
                    String type = ra.assignmentType();
                    String extra = "";

                    if (ra instanceof PermanentAssignment) {
                        PermanentAssignment perm = (PermanentAssignment) ra;
                        extra = String.valueOf(perm.isRevoked());
                    } else if (ra instanceof TemporaryAssignment) {
                        TemporaryAssignment temp = (TemporaryAssignment) ra;
                        extra = temp.getExpiresAt() + "|" + temp.isAutoRenew();
                    }

                    writer.printf("ASSIGNMENT|%s|%s|%s|%s|%s|%s|%s|%s|%s\n",
                            ra.assignmentId(),
                            ra.user().username(),
                            ra.role().getName(),
                            type,
                            ra.isActive(),
                            ra.metadata().assignedBy(),
                            ra.metadata().assignedAt(),
                            ra.metadata().reason(),
                            extra);
                }

                String details = String.format("Сохранено в файл: %s (пользователей: %d, ролей: %d, назначений: %d)",
                        filename, users.size(), roles.size(), assignments.size());

                system.getAuditLog().log(
                        AuditActions.DATA_SAVE,
                        system.getCurrentUser(),
                        "system",
                        details
                );

                ConsoleUtils.printSuccess("Данные успешно сохранены в файл: " + filename);
                ConsoleUtils.printInfo("  - Пользователей: " + users.size());
                ConsoleUtils.printInfo("  - Ролей: " + roles.size());
                ConsoleUtils.printInfo("  - Назначений: " + assignments.size());

            } catch (IOException e) {
                ConsoleUtils.printError("Ошибка при сохранении: " + e.getMessage());

                system.getAuditLog().log(
                        "SAVE_ERROR",
                        system.getCurrentUser(),
                        "system",
                        "Ошибка сохранения: " + e.getMessage()
                );
            }
        });

        // load — загрузить данные из файла
        parser.registerCommand("load", "Загрузить данные из файла", (scanner, system) -> {
            ConsoleUtils.printHeader("ЗАГРУЗКА ДАННЫХ");

            File file = new File(DATA_FILE);
            if (!file.exists()) {
                ConsoleUtils.printError("Файл не найден: " + DATA_FILE);
                return;
            }

            if (!ConsoleUtils.promptYesNo(scanner, "Загрузка данных удалит текущее состояние системы. Продолжить?")) {
                ConsoleUtils.printInfo("Операция отменена");
                return;
            }

            // Очищаем текущие данные
            system.getUserManager().clear();
            system.getRoleManager().clear();
            system.getAssignmentManager().clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                String section = "";
                int userCount = 0, roleCount = 0, assignmentCount = 0;

                Map<String, User> userMap = new HashMap<>();
                Map<String, Role> roleMap = new HashMap<>();
                List<AssignmentData> pendingAssignments = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        if (line.startsWith("#")) {
                            section = line.substring(2).trim();
                        }
                        continue;
                    }

                    String[] parts = line.split("\\|", -1);

                    switch (section) {
                        case "USERS":
                            if (parts.length >= 4 && parts[0].equals("USER")) {
                                try {
                                    User user = User.validate(parts[1], parts[2], parts[3]);
                                    system.getUserManager().add(user);
                                    userMap.put(parts[1], user);
                                    userCount++;
                                } catch (Exception e) {
                                    ConsoleUtils.printWarning("Ошибка загрузки пользователя: " + e.getMessage());
                                }
                            }
                            break;

                        case "ROLES":
                            if (parts.length >= 5 && parts[0].equals("ROLE")) {
                                try {
                                    Role role = new Role(parts[2], parts[3]);

                                    String permsStr = parts[4];
                                    if (!permsStr.isEmpty()) {
                                        String[] perms = permsStr.split(";");
                                        for (String permStr : perms) {
                                            if (!permStr.isEmpty()) {
                                                String[] permParts = permStr.split(":");
                                                if (permParts.length >= 3) {
                                                    Permission perm = new Permission(
                                                            permParts[0], permParts[1], permParts[2]
                                                    );
                                                    role.addPermissions(perm);
                                                }
                                            }
                                        }
                                    }

                                    system.getRoleManager().add(role);
                                    roleMap.put(parts[2], role);
                                    roleCount++;
                                } catch (Exception e) {
                                    ConsoleUtils.printWarning("Ошибка загрузки роли: " + e.getMessage());
                                }
                            }
                            break;

                        case "ASSIGNMENTS":
                            if (parts.length >= 9 && parts[0].equals("ASSIGNMENT")) {
                                pendingAssignments.add(new AssignmentData(parts));
                            }
                            break;
                    }
                }

                for (AssignmentData data : pendingAssignments) {
                    try {
                        User user = userMap.get(data.username);
                        Role role = roleMap.get(data.roleName);

                        if (user == null) {
                            ConsoleUtils.printWarning("Пользователь не найден: " + data.username);
                            continue;
                        }
                        if (role == null) {
                            ConsoleUtils.printWarning("Роль не найдена: " + data.roleName);
                            continue;
                        }

                        AssignmentMetadata metadata = new AssignmentMetadata(
                                data.assignedBy, data.assignedAt, data.reason
                        );

                        RoleAssignment assignment;
                        if (data.type.equals(RoleAssignment.TYPE_PERMANENT)) {
                            boolean revoked = Boolean.parseBoolean(data.extra);
                            assignment = new PermanentAssignment(user, role, metadata);
                        } else {
                            String[] extraParts = data.extra.split("\\|");
                            String expiresAt = extraParts[0];
                            boolean autoRenew = Boolean.parseBoolean(extraParts[1]);
                            assignment = new TemporaryAssignment(
                                    user, role, metadata, expiresAt, autoRenew
                            );
                        }

                        system.getAssignmentManager().add(assignment);
                        assignmentCount++;

                    } catch (Exception e) {
                        ConsoleUtils.printWarning("Ошибка загрузки назначения: " + e.getMessage());
                    }
                }

                ConsoleUtils.printSuccess("Данные успешно загружены из файла: " + DATA_FILE);
                ConsoleUtils.printInfo("  - Загружено пользователей: " + userCount);
                ConsoleUtils.printInfo("  - Загружено ролей: " + roleCount);
                ConsoleUtils.printInfo("  - Загружено назначений: " + assignmentCount);

            } catch (IOException e) {
                ConsoleUtils.printError("Ошибка при загрузке: " + e.getMessage());
            }
        });
    }

    private static class AssignmentData {
        String id;
        String username;
        String roleName;
        String type;
        boolean active;
        String assignedBy;
        String assignedAt;
        String reason;
        String extra;

        AssignmentData(String[] parts) {
            this.id = parts[1];
            this.username = parts[2];
            this.roleName = parts[3];
            this.type = parts[4];
            this.active = Boolean.parseBoolean(parts[5]);
            this.assignedBy = parts[6];
            this.assignedAt = parts[7];
            this.reason = parts[8];
            this.extra = parts.length > 9 ? parts[9] : "";
        }
    }

    private static void printUserDetailedInfo(User user, RBACSystem system) {
        ConsoleUtils.printSubheader("ПОДРОБНАЯ ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ");
        System.out.println(FormatUtils.formatBox(
                "Username: " + user.username() + "\n" +
                        "Full Name: " + user.fullname() + "\n" +
                        "Email: " + user.email()
        ));

        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
        ConsoleUtils.printInfo("Назначенные роли (" + assignments.size() + "):");

        if (assignments.isEmpty()) {
            ConsoleUtils.printInfo("  (нет назначений)");
        } else {
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s] - %s\n",
                        ra.role().getName(),
                        ra.assignmentType(),
                        status);

                if (ra instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) ra;
                    String relativeTime = DateUtils.formatRelativeTime(temp.getExpiresAt().substring(0, 10));
                    System.out.printf("    Истекает: %s (%s)\n", temp.getExpiresAt(), relativeTime);
                }
            }
        }

        Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
        ConsoleUtils.printInfo("Все права пользователя (" + permissions.size() + "):");

        if (permissions.isEmpty()) {
            ConsoleUtils.printInfo("  (нет прав)");
        } else {
            for (Permission p : permissions) {
                System.out.printf("  - %s on %s: %s\n",
                        p.name(), p.resource(), p.description());
            }
        }
    }

    private static void printRoleDetailedInfo(Role role, RBACSystem system) {
        ConsoleUtils.printSubheader("ПОДРОБНАЯ ИНФОРМАЦИЯ О РОЛИ");
        System.out.println(role.format());

        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
        ConsoleUtils.printInfo("Назначена пользователям (" + assignments.size() + "):");

        if (assignments.isEmpty()) {
            ConsoleUtils.printInfo("  (нет назначений)");
        } else {
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s]\n",
                        ra.user().username(),
                        status);
            }
        }
    }

    private static void printAssignmentDetails(RoleAssignment ra) {
        System.out.println("\n──────────────────────────────────");
        System.out.println("ID: " + ra.assignmentId());
        System.out.println("Пользователь: " + ra.user().username());
        System.out.println("Роль: " + ra.role().getName());
        System.out.println("Тип: " + ra.assignmentType());
        System.out.println("Статус: " + (ra.isActive() ? "ACTIVE" : "INACTIVE"));
        System.out.println("Назначил: " + ra.metadata().assignedBy());
        System.out.println("Дата назначения: " + ra.metadata().assignedAt());

        if (!ra.metadata().reason().isEmpty()) {
            System.out.println("Причина: " + ra.metadata().reason());
        }

        if (ra instanceof TemporaryAssignment) {
            TemporaryAssignment temp = (TemporaryAssignment) ra;
            String relativeTime = DateUtils.formatRelativeTime(temp.getExpiresAt().substring(0, 10));
            System.out.println("Истекает: " + temp.getExpiresAt() + " (" + relativeTime + ")");
            System.out.println("Осталось: " + temp.getTimeRemaining());
        }
    }

    private static void addPermissionsToRole(Role role, Scanner scanner, RBACSystem system) {
        while (ConsoleUtils.promptYesNo(scanner, "\nХотите добавить права к роли?")) {
            ConsoleUtils.printSubheader("Введите данные права");

            String permName = ConsoleUtils.promptString(scanner, "Название права (READ, WRITE, DELETE и т.д.)", true).toUpperCase();
            String resource = ConsoleUtils.promptString(scanner, "Ресурс (users, roles, reports и т.д.)", true).toLowerCase();
            String permDescription = ConsoleUtils.promptString(scanner, "Описание", true);

            try {
                Permission permission = new Permission(permName, resource, permDescription);
                system.getRoleManager().addPermissionToRole(role.getName(), permission);
                ConsoleUtils.printSuccess("Право добавлено!");
            } catch (Exception e) {
                ConsoleUtils.printError("Ошибка: " + e.getMessage());
            }
        }
    }
}