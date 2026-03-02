package command;

import filters.*;
import interfaces.RoleAssignment;
import record.Permission;
import record.User;
import role.PermanentAssignment;
import role.Role;
import record.AssignmentMetadata;
import role.TemporaryAssignment;

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

    private static void registerUserCommands (CommandParser parser) {
        // user-list - список всех пользователей
        parser.registerCommand("user-list", "Вывести список всех пользователей", ((scanner, system) -> {
            System.out.println("Список всех пользователей");
            List<User> users = system.getUserManager().findAll();

            if (users.isEmpty()) {
                System.out.println("В системе нет пользователей");
                return;
            }
            printUserTable(users);
        }));

        // user-create - создать нового пользователя
        parser.registerCommand("user-create", "Создание нового пользователя", ((scanner, system) -> {
            System.out.println("Создание нового пользователя");

            System.out.println("Введите имя пользователя: ");
            String username = scanner.nextLine().trim();

            System.out.println("Введите полное имя пользователя: ");
            String fullName = scanner.nextLine().trim();

            System.out.println("Введите email пользователя: ");
            String email = scanner.nextLine().trim();

            try {
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("Пользователь успешно создан");
                System.out.println(" " + user.format());
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка валидации: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }));

        // user-view - просмотр информации о пользователе
        parser.registerCommand("user-view", "Просмотр информации о пользователе", ((scanner, system) ->  {
            System.out.println("Просмотр пользователя");

            System.out.println("Введите имя пользователя: ");
            String username = scanner.nextLine().trim();

            system.getUserManager().findByUsername(username).ifPresentOrElse(
                    user -> printUserDetailedInfo(user, system),
                    () -> System.out.println("Пользователь " + username + " не найден"));
        }));

        // user-update - обновить данные пользователя
        parser.registerCommand("user-update", "Обновить данные пользователя", ((scanner, system) -> {
            System.out.println("Обновление пользователя");

            System.out.println("Введите имя пользователя: ");
            String username = scanner.nextLine().trim();

            if (system.getUserManager().exists(username)) {
                System.out.println("Пользователь " + username + " не найден");
                return;
            }

            System.out.println("Введите новое полное имя пользователя");
            String newFullname = scanner.nextLine().trim();

            System.out.println("Введите новый email пользователя");
            String newEmail = scanner.nextLine().trim();

            User currentUser = system.getUserManager().findByUsername(username).get();

            if (newFullname.isEmpty()) {
                newFullname = currentUser.fullname();
            }

            if (newEmail.isEmpty()) {
                newEmail = currentUser.email();
            }

            try {
                system.getUserManager().update(username, newFullname, newEmail);
                System.out.println("Данные пользователя " + username + " обновлены");
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка + " + e.getMessage());
            }
        }));

        // user-delete - удалить пользователя
        parser.registerCommand("user-delete", "Удалить пользователя", ((scanner, system) -> {
            System.out.println("Удаление пользователя");

            System.out.println("Введите имя пользователя: ");
            String username = scanner.nextLine().trim();

            User user = system.getUserManager().findByUsername(username).orElse(null);

            if (user == null) {
                System.out.println("Пользователь " + username + " не найден");
            }

            if (user.equals(system.getCurrentUser())) {
                System.out.println("Невозможно удалить пользователя системы");
                return;
            }
            System.out.println("Вы уверены? (введите 'да' для подтверждения): ");
            String confirmator = scanner.nextLine();

            if (!confirmator.equalsIgnoreCase("да")) {
                System.out.println("Операция отменена");
                return;
            }

            List<RoleAssignment> userAssignment = system.getAssignmentManager().findByUser(user);

            for (RoleAssignment assignment : userAssignment) {
                system.getAssignmentManager().remove(assignment);
            }

            boolean removed = system.getUserManager().remove(user);

            if (removed) {
                System.out.println("Пользователь " + username + " удален");
                System.out.println("Удалено " + userAssignment.size() + " назначений");
            } else {
            System.out.println("Ошибка при удалении пользователя");
            }
        }));

        // user-search - поиск пользователей по фильтрам
        parser.registerCommand("user-search", "Поиск пользователей по фильтрам", ((scanner, system) -> {
            System.out.println("Поиск пользователей");
            System.out.println("Выберите тип фильтра:");
            System.out.println("1 - По username (содержит)");
            System.out.println("2 - По email (содержит)");
            System.out.println("3 - По домену email");
            System.out.println("4 - По полному имени (содержит)");
            System.out.println("0 - Отмена");

            System.out.println("Ваш выбор: ");
            String choice = scanner.nextLine().trim();

            UserFilter filter = null;
            String filterDescription = "";

            switch (choice) {
                case "1":
                    System.out.print("Введите подстроку для поиска в username: ");
                    String usernameSub = scanner.nextLine().trim();
                    filter = UserFilters.byUsernameContains(usernameSub);
                    filterDescription = "username содержит '" + usernameSub + "'";
                    break;

                case "2":
                    System.out.print("Введите подстроку для поиска в email: ");
                    String emailSub = scanner.nextLine().trim();
                    filter = UserFilters.byEmail(emailSub);
                    filterDescription = "email содержит '" + emailSub + "'";
                    break;

                case "3":
                    System.out.print("Введите домен email (например, @gmail.com): ");
                    String domain = scanner.nextLine().trim();
                    filter = UserFilters.byEmailDomain(domain);
                    filterDescription = "email домен '" + domain + "'";
                    break;

                case "4":
                    System.out.print("Введите подстроку для поиска в полном имени: ");
                    String nameSub = scanner.nextLine().trim();
                    filter = UserFilters.byFullNameContains(nameSub);
                    filterDescription = "полное имя содержит '" + nameSub + "'";
                    break;

                case "0":
                    System.out.println("Поиск отменен");
                    return;

                default:
                    System.out.println("Неверный выбор");
                    return;
            }

            // Выполняем поиск
            List<User> results = system.getUserManager().findByFilter(filter);

            System.out.println("Результаты поиска: ");
            System.out.println("Фильтр: " + filterDescription);
            System.out.println("Найдено пользователей: " + results.size());

            if (!results.isEmpty()) {
                printUserTable(results);
            }
        }));
    }

    private static void registerRoleCommands(CommandParser parser) {

        // role-list — вывести список всех ролей
        parser.registerCommand("role-list", "Вывести список всех ролей", (scanner, system) -> {
            System.out.println("Все роли: ");
            List<Role> roles = system.getRoleManager().findAll();

            if (roles.isEmpty()) {
                System.out.println("В системе нет ролей");
            }
            printRoleTable(roles);
        });


        // role-create — создать новую роль

        parser.registerCommand("role-create", "Создать новую роль", ((scanner, system) -> {
            System.out.println("Создание новой роли");

            System.out.println("Введите название роли: ");
            String roleName = scanner.nextLine().trim();

            System.out.println("Введите описание роли: ");
            String description = scanner.nextLine().trim();

            try {
                Role role = new Role(roleName, description);
                system.getRoleManager().add(role);
                System.out.println("Новая роль " + roleName + " создана");

                addPermissionsToRole(role, scanner, system);
            } catch (IllegalStateException e) {
                System.out.println("Ошибка " + e.getMessage());
            }
        }));

        // role-view — просмотр роли
        parser.registerCommand("role-view", "Просмотр информации о роли", (scanner, system) ->  {
            System.out.println("Просмотр информации о роли: ");

            System.out.println("Введите название роли: ");
            String roleName = scanner.nextLine().trim();

            system.getRoleManager().findByName(roleName).ifPresentOrElse(
                    role -> printRoleDetailedInfo(role, system),
                    () -> System.out.println("Роль " + roleName + " не найдена")
                    );
        });

        // role-update — обновить роль
        parser.registerCommand("role-update", "Обновить название и описание роли", (scanner, system) ->  {
            System.out.println("Обновление данных роли");

            System.out.println("Введите название роли: ");
            String oldName = scanner.nextLine().trim();

            Role role = system.getRoleManager().findByName(oldName).orElse(null);

            if (role == null) {
                System.out.println("Роль " + oldName + " не найдена");
                return;
            }

            System.out.println("Введите новое название роли: ");
            String newName= scanner.nextLine().trim();

            if (newName.isEmpty()) {
                newName = oldName;
            }

            System.out.println("Введите новое описание роли: ");
            String newDescription = scanner.nextLine().trim();

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
                System.out.println("Роль обновлена");
            } catch (Exception e) {
                System.out.println("Ошибка " + e.getMessage());
                System.out.println("Попытка восстановить роль");
                try {
                    system.getRoleManager().add(role);
                    System.out.println("Роль восстановлена");
                } catch (Exception ex) {
                    System.out.println("Ошибка " + ex.getMessage());
                }
            }
        });

        // role-delete — удалить роль
        parser.registerCommand("role-delete", "Удаление роли", ((scanner, system) ->  {
            System.out.println("Удаление роли");
            System.out.println("Введите название роли: ");
            String roleName = scanner.nextLine().trim();

            Role role = system.getRoleManager().findByName(roleName).orElse(null);

            if (role == null) {
                System.out.println("Роль " + roleName + " не найдена");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
            List<RoleAssignment> activeAssignments = assignments.stream().filter(RoleAssignment::isActive).collect(Collectors.toList());

            if (!activeAssignments.isEmpty()) {
                System.out.println("Роль назначена пользователям");
                System.out.println("Активных назначений: " + activeAssignments.size());

                for (RoleAssignment ra : activeAssignments) {
                    System.out.printf("  - %s (%s)\n",
                            ra.user().username(),
                            ra.assignmentType());
                }

                System.out.print("\nВведите 'да' для принудительного удаления (назначения будут отозваны): ");
                String confirmation = scanner.nextLine().trim();

                if (!confirmation.equalsIgnoreCase("да")) {
                    System.out.println("Операция отменена");
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
                    System.out.println("Роль " + roleName + " удалена");
                } else {
                    System.out.println("Ошибка при удалении роли");
                }
            } catch (IllegalStateException e) {
                System.out.println("Ошибка " + e.getMessage());
            }
        }));

        // role-add-permission — добавить право к роли
        parser.registerCommand("role-add-permission", "Добавить право к роли", (scanner, system) -> {
            System.out.println("Добавление права к роли");

            System.out.print("Введите название роли: ");
            String roleName = scanner.nextLine().trim();

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                System.out.println("Роль " + roleName + " не найдена");
                return;
            }

            System.out.println("Введите данные права:");
            System.out.println("Название права (READ, WRITE, DELETE и т.д.): ");
            String permName = scanner.nextLine().trim().toUpperCase();

            System.out.print("Ресурс (users, roles, reports и т.д.): ");
            String resource = scanner.nextLine().trim().toLowerCase();

            System.out.print("Описание: ");
            String permDescription = scanner.nextLine().trim();

            try {
                Permission permission = new Permission(permName, resource, permDescription);
                system.getRoleManager().addPermissionToRole(roleName, permission);
                System.out.println("Право добавлено к роли '" + roleName + "'");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        // role-remove-permission — удалить право из роли
        parser.registerCommand("role-remove-permission", "Удалить право из роли", (scanner, system) -> {
            System.out.println("Удаление права из роли");

            System.out.print("Введите название роли: ");
            String roleName = scanner.nextLine().trim();

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                System.out.println("Роль " + roleName + " не найдена");
                return;
            }

            Set<Permission> permissions = role.getPermissions();
            if (permissions.isEmpty()) {
                System.out.println("У роли нет прав для удаления");
                return;
            }

            System.out.println("Права роли " + roleName + ":");
            List<Permission> permList = new ArrayList<>(permissions);
            for (int i = 0; i < permList.size(); i++) {
                Permission p = permList.get(i);
                System.out.printf("  %d. %s on %s: %s\n",
                        i + 1, p.name(), p.resource(), p.description());
            }

            System.out.print("\nВведите номер права для удаления (0 - отмена): ");
            String choice = scanner.nextLine().trim();

            try {
                int index = Integer.parseInt(choice) - 1;
                if (index == -1) {
                    System.out.println("Операция отменена");
                    return;
                }

                if (index >= 0 && index < permList.size()) {
                    Permission toRemove = permList.get(index);
                    system.getRoleManager().removePermissionFromRole(roleName, toRemove);
                    System.out.println("Право удалено");
                } else {
                    System.out.println("Неверный номер");
                }
            } catch (NumberFormatException e) {
                System.out.println("Введите число");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        // role-search — поиск ролей
        parser.registerCommand("role-search", "Поиск ролей по фильтрам", (scanner, system) -> {
            System.out.println("ПОИСК РОЛЕЙ");
            System.out.println("Выберите тип фильтра:");
            System.out.println("1 - По названию (содержит)");
            System.out.println("2 - По наличию конкретного права");
            System.out.println("3 - По минимальному количеству прав");
            System.out.println("0 - Отмена");

            System.out.print("Ваш выбор: ");
            String choice = scanner.nextLine().trim();

            RoleFilter filter = null;
            String filterDescription = "";

            switch (choice) {
                case "1":
                    System.out.print("Введите подстроку для поиска в названии: ");
                    String nameSub = scanner.nextLine().trim();
                    filter = RoleFilters.byNameContains(nameSub);
                    filterDescription = "название содержит '" + nameSub + "'";
                    break;

                case "2":
                    System.out.print("Введите название права (READ, WRITE и т.д.): ");
                    String permName = scanner.nextLine().trim().toUpperCase();

                    System.out.print("Введите ресурс: ");
                    String resource = scanner.nextLine().trim().toLowerCase();

                    filter = RoleFilters.hasPermission(permName, resource);
                    filterDescription = "имеет право " + permName + " на " + resource;
                    break;

                case "3":
                    System.out.print("Введите минимальное количество прав: ");
                    try {
                        int minCount = Integer.parseInt(scanner.nextLine().trim());
                        filter = RoleFilters.hasAtLeastNPermissions(minCount);
                        filterDescription = "минимум " + minCount + " прав";
                    } catch (NumberFormatException e) {
                        System.out.println("Введите число");
                        return;
                    }
                    break;

                case "0":
                    System.out.println("Поиск отменен");
                    return;

                default:
                    System.out.println("Неверный выбор");
                    return;
            }

            // Выполняем поиск
            List<Role> results = system.getRoleManager().findByFilter(filter);

            System.out.println("\nРЕЗУЛЬТАТЫ ПОИСКА ");
            System.out.println("Фильтр: " + filterDescription);
            System.out.println("Найдено ролей: " + results.size());

            if (!results.isEmpty()) {
                printRoleTable(results);
            }
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {

        // assign-role — назначить роль пользователю
        parser.registerCommand("assign-role", "Назначить роль пользователю", (scanner, system) -> {
            System.out.println("Назначение роли пользователю");

            System.out.print("Введите username пользователя: ");
            String username = scanner.nextLine().trim();

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                System.out.println("Пользователь " + username + " не найден");
                return;
            }

            List<Role> availableRoles = system.getRoleManager().findAll();
            if (availableRoles.isEmpty()) {
                System.out.println("В системе нет доступных ролей");
                return;
            }

            System.out.println("Доступные роли:");
            for (int i = 0; i < availableRoles.size(); i++) {
                Role role = availableRoles.get(i);
                System.out.printf("  %d. %s - %s (прав: %d)\n",
                        i + 1, role.getName(), role.getDescription(),
                        role.getPermissions().size());
            }

            System.out.print("\nВыберите номер роли (0 - отмена): ");
            String roleChoice = scanner.nextLine().trim();

            try {
                int roleIndex = Integer.parseInt(roleChoice) - 1;
                if (roleIndex == -1) {
                    System.out.println("Операция отменена");
                    return;
                }

                if (roleIndex < 0 || roleIndex >= availableRoles.size()) {
                    System.out.println("Неверный номер роли");
                    return;
                }

                Role selectedRole = availableRoles.get(roleIndex);

                if (system.getAssignmentManager().hasActiveAssignment(username, selectedRole.getName())) {
                    System.out.println("У пользователя уже есть активное назначение на эту роль");
                    return;
                }

                System.out.println("\nВыберите тип назначения:");
                System.out.println("  1 - Постоянное");
                System.out.println("  2 - Временное");
                System.out.print("Ваш выбор: ");

                String typeChoice = scanner.nextLine().trim();

                RoleAssignment assignment = null;
                AssignmentMetadata metadata = null;


                System.out.print("Введите причину назначения: ");
                String reason = scanner.nextLine().trim();
                if (reason.isEmpty()) {
                    reason = "Назначено через консоль";
                }

                metadata = new AssignmentMetadata(
                        system.getCurrentUser(),
                        LocalDateTime.now().format(FORMATTER),
                        reason
                );

                switch (typeChoice) {
                    case "1": // Постоянное
                        assignment = new PermanentAssignment(user, selectedRole, metadata);
                        break;

                    case "2": // Временное
                        System.out.print("Введите дату истечения (формат: yyyy-MM-dd HH:mm): ");
                        String expiryDate = scanner.nextLine().trim();

                        try {
                            LocalDateTime.parse(expiryDate, FORMATTER);
                            assignment = new TemporaryAssignment(
                                    user, selectedRole, metadata, expiryDate, false
                            );
                        } catch (DateTimeParseException e) {
                            System.out.println("Неверный формат даты");
                            return;
                        }
                        break;

                    default:
                        System.out.println("Неверный тип назначения");
                        return;
                }

                system.getAssignmentManager().add(assignment);
                System.out.println("Роль успешно назначена!");
                System.out.println("ID назначения: " + assignment.assignmentId());

            } catch (NumberFormatException e) {
                System.out.println("Введите число");
            } catch (IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        // revoke-role — отозвать роль у пользователя
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя", (scanner, system) -> {
            System.out.println("Отзыв роли у пользователя");

            System.out.print("Введите username пользователя: ");
            String username = scanner.nextLine().trim();

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                System.out.println("Пользователь '" + username + "' не найден");
                return;
            }

            List<RoleAssignment> activeAssignments = system.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (activeAssignments.isEmpty()) {
                System.out.println("У пользователя нет активных назначений");
                return;
            }

            System.out.println("\nАктивные назначения пользователя:");
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

            System.out.print("\nВыберите номер назначения для отзыва (0 - отмена): ");
            String choice = scanner.nextLine().trim();

            try {
                int index = Integer.parseInt(choice) - 1;
                if (index == -1) {
                    System.out.println("Операция отменена");
                    return;
                }

                if (index >= 0 && index < activeAssignments.size()) {
                    RoleAssignment toRevoke = activeAssignments.get(index);

                    if (toRevoke instanceof PermanentAssignment) {
                        // Для постоянных - revoke
                        system.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
                        System.out.println("Назначение отозвано");
                    } else {
                        // Для временных - пока не реализовано
                        System.out.println("Отмена временных назначений пока не поддерживается");
                    }
                } else {
                    System.out.println("Неверный номер");
                }
            } catch (NumberFormatException e) {
                System.out.println("Введите число");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        // assignment-list — список всех назначений
        parser.registerCommand("assignment-list", "Список всех назначений", (scanner, system) -> {
            List<RoleAssignment> assignments = system.getAssignmentManager().findAll();

            if (assignments.isEmpty()) {
                System.out.println("В системе нет назначений");
                return;
            }

            printAssignmentTable(assignments, system);
        });

        // assignment-list-user — назначения конкретного пользователя
        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя", (scanner, system) -> {
            System.out.println("Назначения пользователя");

            System.out.print("Введите username: ");
            String username = scanner.nextLine().trim();

            User user = system.getUserManager().findByUsername(username).orElse(null);
            if (user == null) {
                System.out.println("Пользователь '" + username + "' не найден");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);

            if (assignments.isEmpty()) {
                System.out.println("У пользователя нет назначений");
                return;
            }

            System.out.println("\nНазначения пользователя " + username + ":");
            for (RoleAssignment ra : assignments) {
                printAssignmentDetails(ra);
            }
        });

        // assignment-list-role — список пользователей с конкретной ролью
        parser.registerCommand("assignment-list-role", "Список пользователей с ролью", (scanner, system) -> {
            System.out.println("Пользователи с ролью");

            System.out.print("Введите имя роли: ");
            String roleName = scanner.nextLine().trim();

            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            if (role == null) {
                System.out.println("Роль '" + roleName + "' не найдена");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);

            if (assignments.isEmpty()) {
                System.out.println("Нет пользователей с ролью '" + roleName + "'");
                return;
            }

            System.out.println("\nПользователи с ролью " + roleName + ":");
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
            List<RoleAssignment> active = system.getAssignmentManager().getActiveAssignments();

            if (active.isEmpty()) {
                System.out.println("Нет активных назначений");
                return;
            }

            System.out.println("\n=== АКТИВНЫЕ НАЗНАЧЕНИЯ ===");
            printAssignmentTable(active, system);
        });

        // assignment-expired — истёкшие временные назначения
        parser.registerCommand("assignment-expired", "Истекшие временные назначения", (scanner, system) -> {
            List<RoleAssignment> expired = system.getAssignmentManager().getExpiredAssignments().stream()
                    .filter(ra -> ra instanceof TemporaryAssignment)
                    .collect(Collectors.toList());

            if (expired.isEmpty()) {
                System.out.println("Нет истекших назначений");
                return;
            }

            System.out.println("Истекшие временные назначения");
            printAssignmentTable(expired, system);
        });

        // assignment-extend — продлить временное назначение
        parser.registerCommand("assignment-extend", "Продлить временное назначение", (scanner, system) -> {
            System.out.println("Продление назначения");

            System.out.println("Выберите способ поиска назначения:");
            System.out.println("  1 - По ID назначения");
            System.out.println("  2 - По пользователю + роли");
            System.out.print("Ваш выбор: ");

            String choice = scanner.nextLine().trim();
            String assignmentId = null;

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Введите ID назначения: ");
                        assignmentId = scanner.nextLine().trim();
                        break;

                    case "2":
                        System.out.print("Введите username: ");
                        String username = scanner.nextLine().trim();

                        System.out.print("Введите имя роли: ");
                        String roleName = scanner.nextLine().trim();

                        // Ищем активное назначение
                        List<RoleAssignment> assignments = system.getAssignmentManager().findByUsername(username).stream()
                                .filter(ra -> ra.role().getName().equals(roleName))
                                .filter(RoleAssignment::isActive)
                                .collect(Collectors.toList());

                        if (assignments.isEmpty()) {
                            System.out.println("Активное назначение не найдено");
                            return;
                        }

                        if (assignments.size() > 1) {
                            System.out.println("Найдено несколько назначений. Используйте поиск по ID.");
                            return;
                        }

                        assignmentId = assignments.get(0).assignmentId();
                        break;

                    default:
                        System.out.println("Неверный выбор");
                        return;
                }

                // Проверяем, что назначение существует и временное
                RoleAssignment assignment = system.getAssignmentManager().findById(assignmentId).orElse(null);
                if (assignment == null) {
                    System.out.println("Назначение не найдено");
                    return;
                }

                if (!(assignment instanceof TemporaryAssignment)) {
                    System.out.println("Можно продлевать только временные назначения");
                    return;
                }

                System.out.print("Введите новую дату истечения (формат: yyyy-MM-dd HH:mm): ");
                String newDate = scanner.nextLine().trim();

                system.getAssignmentManager().extendTemporaryAssignment(assignmentId, newDate);
                System.out.println("Назначение продлено до " + newDate);

            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });

        // assignment-search — поиск назначений по фильтрам
        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам", (scanner, system) -> {
            System.out.println("Поиск назначений");
            System.out.println("Выберите тип фильтра:");
            System.out.println("  1 - По пользователю");
            System.out.println("  2 - По роли");
            System.out.println("  3 - По типу (постоянное/временное)");
            System.out.println("  4 - По статусу (активное/неактивное)");
            System.out.println("  5 - Назначенные после даты");
            System.out.println("  6 - Истекающие до даты");
            System.out.println("  0 - Отмена");

            System.out.print("Ваш выбор: ");
            String filterChoice = scanner.nextLine().trim();

            AssignmentFilter filter = null;
            String filterDescription = "";

            try {
                switch (filterChoice) {
                    case "1": // По пользователю
                        System.out.print("Введите username: ");
                        String username = scanner.nextLine().trim();
                        filter = AssignmentFilters.byUsername(username);
                        filterDescription = "пользователь = " + username;
                        break;

                    case "2": // По роли
                        System.out.print("Введите имя роли: ");
                        String roleName = scanner.nextLine().trim();
                        filter = AssignmentFilters.byRoleName(roleName);
                        filterDescription = "роль = " + roleName;
                        break;

                    case "3": // По типу
                        System.out.println("Выберите тип:");
                        System.out.println("  1 - PERMANENT (постоянные)");
                        System.out.println("  2 - TEMPORARY (временные)");
                        System.out.print("Ваш выбор: ");

                        String typeChoice = scanner.nextLine().trim();
                        if (typeChoice.equals("1")) {
                            filter = AssignmentFilters.byType(RoleAssignment.TYPE_PERMANENT);
                            filterDescription = "тип = PERMANENT";
                        } else if (typeChoice.equals("2")) {
                            filter = AssignmentFilters.byType(RoleAssignment.TYPE_TEMPORARY);
                            filterDescription = "тип = TEMPORARY";
                        } else {
                            System.out.println("Неверный выбор");
                            return;
                        }
                        break;

                    case "4": // По статусу
                        System.out.println("Выберите статус:");
                        System.out.println("  1 - ACTIVE (активные)");
                        System.out.println("  2 - INACTIVE (неактивные)");
                        System.out.print("Ваш выбор: ");

                        String statusChoice = scanner.nextLine().trim();
                        if (statusChoice.equals("1")) {
                            filter = AssignmentFilters.activeOnly();
                            filterDescription = "статус = ACTIVE";
                        } else if (statusChoice.equals("2")) {
                            filter = AssignmentFilters.inactiveOnly();
                            filterDescription = "статус = INACTIVE";
                        } else {
                            System.out.println("Неверный выбор");
                            return;
                        }
                        break;

                    case "5": // Назначенные после даты
                        System.out.print("Введите дату (формат: yyyy-MM-dd HH:mm): ");
                        String afterDate = scanner.nextLine().trim();
                        filter = AssignmentFilters.assignedAfter(afterDate);
                        filterDescription = "назначено после " + afterDate;
                        break;

                    case "6": // Истекающие до даты
                        System.out.print("Введите дату (формат: yyyy-MM-dd HH:mm): ");
                        String beforeDate = scanner.nextLine().trim();
                        filter = AssignmentFilters.expiringBefore(beforeDate);
                        filterDescription = "истекает до " + beforeDate;
                        break;

                    case "0":
                        System.out.println("Поиск отменен");
                        return;

                    default:
                        System.out.println("Неверный выбор");
                        return;
                }

                // Выполняем поиск
                List<RoleAssignment> results = system.getAssignmentManager().findByFilter(filter);

                System.out.println("Результаты поиска");
                System.out.println("Фильтр: " + filterDescription);
                System.out.println("Найдено назначений: " + results.size());

                if (!results.isEmpty()) {
                    printAssignmentTable(results, system);
                }

            } catch (DateTimeParseException e) {
                System.out.println("Неверный формат даты");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        });
    }

    private static void registerUtilityCommands(CommandParser parser) {

        // help — справка по командам (уже зарегистрирована, но обновим описание)
        parser.registerCommand("help", "Показать список всех команд", (scanner, system) -> {
            parser.printHelp();
        });

        // stats — статистика системы
        parser.registerCommand("stats", "Показать расширенную статистику системы", (scanner, system) -> {
            System.out.println("Статичстика системы");

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

            System.out.printf("Среднее количество ролей на пользователя: %.2f\n", avgRolesPerUser);

            // Топ-3 самых популярных ролей
            System.out.println("\nТоп-3 самых популярных ролей:");

            Map<String, Integer> rolePopularity = new HashMap<>();
            for (RoleAssignment ra : system.getAssignmentManager().findAll()) {
                String roleName = ra.role().getName();
                rolePopularity.put(roleName, rolePopularity.getOrDefault(roleName, 0) + 1);
            }

            rolePopularity.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> System.out.printf("  %d. %s (%d назначений)\n",
                            rolePopularity.size() > 3 ?
                                    new ArrayList<>(rolePopularity.keySet()).indexOf(entry.getKey()) + 1 :
                                    new ArrayList<>(rolePopularity.keySet()).indexOf(entry.getKey()) + 1,
                            entry.getKey(), entry.getValue()));

            if (rolePopularity.isEmpty()) {
                System.out.println("  (нет данных)");
            }

            System.out.println("\n__________________________________");
        });

        // clear — очистить экран
        parser.registerCommand("clear", "Очистить экран", (scanner, system) -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        });

        // exit — выход из программы
        parser.registerCommand("exit", "Выйти из программы", (scanner, system) -> {
            System.out.println("Выход из программы");

            System.out.print("Сохранить данные перед выходом? (да/нет): ");
            String saveChoice = scanner.nextLine().trim();

            if (saveChoice.equalsIgnoreCase("да")) {
                parser.executeCommand("save", scanner, system);
            }

            System.out.print("Вы уверены, что хотите выйти? (да/нет): ");
            String confirm = scanner.nextLine().trim();

            if (confirm.equalsIgnoreCase("да")) {
                System.out.println("\nДо свидания!");
                System.exit(0);
            } else {
                System.out.println("Выход отменен");
            }
        });

        // save — сохранить данные в файл
        parser.registerCommand("save", "Сохранить данные в файл", (scanner, system) -> {
            System.out.println("Сохранение данных");

            try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {

                // Сохраняем пользователей
                writer.println("# USERS");
                List<User> users = system.getUserManager().findAll();
                for (User user : users) {
                    writer.printf("USER|%s|%s|%s\n",
                            user.username(), user.fullname(), user.email());
                }
                writer.println();

                // Сохраняем роли и их права
                writer.println("# ROLES");
                List<Role> roles = system.getRoleManager().findAll();
                for (Role role : roles) {
                    // Формируем строку с правами: name:resource:description;
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

                // Сохраняем назначения
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

                    writer.printf("ASSIGNMENT|%s|%s|%s|%s|%s|%s|%s|%s\n",
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

                System.out.println("Данные успешно сохранены в файл: " + DATA_FILE);
                System.out.println("  - Пользователей: " + users.size());
                System.out.println("  - Ролей: " + roles.size());
                System.out.println("  - Назначений: " + assignments.size());

            } catch (IOException e) {
                System.out.println("Ошибка при сохранении: " + e.getMessage());
            }
        });

        // load — загрузить данные из файла
        parser.registerCommand("load", "Загрузить данные из файла", (scanner, system) -> {
            System.out.println("Загрузка данных");

            File file = new File(DATA_FILE);
            if (!file.exists()) {
                System.out.println("Файл не найден: " + DATA_FILE);
                return;
            }

            System.out.print("Загрузка данных удалит текущее состояние системы. Продолжить? (да/нет): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("да")) {
                System.out.println("Операция отменена");
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

                // Временные хранилища для связывания данных
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
                                    System.out.println("Ошибка загрузки пользователя: " + e.getMessage());
                                }
                            }
                            break;

                        case "ROLES":
                            if (parts.length >= 5 && parts[0].equals("ROLE")) {
                                try {
                                    // parts: ROLE|id|name|description|permissions
                                    Role role = new Role(parts[2], parts[3]);

                                    // Парсим права
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
                                    System.out.println("Ошибка загрузки роли: " + e.getMessage());
                                }
                            }
                            break;

                        case "ASSIGNMENTS":
                            if (parts.length >= 9 && parts[0].equals("ASSIGNMENT")) {
                                // Сохраняем данные для последующего создания назначений
                                // после того, как все пользователи и роли загружены
                                pendingAssignments.add(new AssignmentData(parts));
                            }
                            break;
                    }
                }

                // Создаем назначения после загрузки всех пользователей и ролей
                for (AssignmentData data : pendingAssignments) {
                    try {
                        User user = userMap.get(data.username);
                        Role role = roleMap.get(data.roleName);

                        if (user == null) {
                            System.out.println("Пользователь не найден: " + data.username);
                            continue;
                        }
                        if (role == null) {
                            System.out.println("Роль не найдена: " + data.roleName);
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

                        // Добавляем в менеджер (обходим проверки, т.к. данные уже валидны)
                        system.getAssignmentManager().add(assignment);
                        assignmentCount++;

                    } catch (Exception e) {
                        System.out.println("Ошибка загрузки назначения: " + e.getMessage());
                    }
                }

                System.out.println("Данные успешно загружены из файла: " + DATA_FILE);
                System.out.println("  - Загружено пользователей: " + userCount);
                System.out.println("  - Загружено ролей: " + roleCount);
                System.out.println("  - Загружено назначений: " + assignmentCount);

            } catch (IOException e) {
                System.out.println("Ошибка при загрузке: " + e.getMessage());
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
            // ASSIGNMENT|id|username|roleName|type|active|assignedBy|assignedAt|reason|extra
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

    private static void printUserTable(List<User> users) {
        System.out.println("\n┌────────────────────────────────────────────────────────────────┐");
        System.out.println("│                           ПОЛЬЗОВАТЕЛИ                         │");
        System.out.println("├──────────────┬──────────────────────────┬───────────────────────┤");
        System.out.println("│ Username     │ Full Name                │ Email                 │");
        System.out.println("├──────────────┼──────────────────────────┼───────────────────────┤");

        for (User user : users) {
            System.out.printf("│ %-12s │ %-24s │ %-21s │\n",
                    truncate(user.username(), 12),
                    truncate(user.fullname(), 24),
                    truncate(user.email(), 21));
        }

        System.out.println("└──────────────┴──────────────────────────┴───────────────────────┘");
        System.out.println("Всего пользователей: " + users.size());
    }

    private static void printRoleTable(List<Role> roles) {
        System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│                           РОЛИ                                │");
        System.out.println("├──────────────────────┬──────────────┬────────────────────────┤");
        System.out.println("│ Название             │ Прав         │ ID                     │");
        System.out.println("├──────────────────────┼──────────────┼────────────────────────┤");

        for (Role role : roles) {
            System.out.printf("│ %-20s │ %-12d │ %-22s │\n",
                    truncate(role.getName(), 20),
                    role.getPermissions().size(),
                    truncate(role.getId(), 22));
        }

        System.out.println("└──────────────────────┴──────────────┴────────────────────────┘");
        System.out.println("Всего ролей: " + roles.size());
    }

    private static void printUserDetailedInfo(User user, RBACSystem system) {
        System.out.println("\n=== ПОДРОБНАЯ ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ ===");
        System.out.println("Username:     " + user.username());
        System.out.println("Full Name:    " + user.fullname());
        System.out.println("Email:        " + user.email());

        // Получаем назначения пользователя
        List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
        System.out.println("\nНазначенные роли (" + assignments.size() + "):");

        if (assignments.isEmpty()) {
            System.out.println("  (нет назначений)");
        } else {
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s] - %s\n",
                        ra.role().getName(),
                        ra.assignmentType(),
                        status);

                if (ra instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) ra;
                    System.out.printf("    Истекает: %s\n", temp.getExpiresAt());
                }
            }
        }

        // Получаем все права пользователя
        Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
        System.out.println("\nВсе права пользователя (" + permissions.size() + "):");

        if (permissions.isEmpty()) {
            System.out.println("  (нет прав)");
        } else {
            for (Permission p : permissions) {
                System.out.printf("  - %s on %s: %s\n",
                        p.name(), p.resource(), p.description());
            }
        }
    }

    private static void printRoleDetailedInfo(Role role, RBACSystem system) {
        System.out.println("\n=== ПОДРОБНАЯ ИНФОРМАЦИЯ О РОЛИ ===");
        System.out.println(role.format());

        // Получаем назначения роли
        List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
        System.out.println("\nНазначена пользователям (" + assignments.size() + "):");

        if (assignments.isEmpty()) {
            System.out.println("  (нет назначений)");
        } else {
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s]\n",
                        ra.user().username(),
                        status);
            }
        }
    }

    private static void printAssignmentTable(List<RoleAssignment> assignments, RBACSystem system) {
        System.out.println("\n┌────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                         НАЗНАЧЕНИЯ                                         │");
        System.out.println("├──────────────┬──────────────────┬────────────┬──────────┬────────────────────┬────────────┤");
        System.out.println("│ Пользователь │ Роль             │ Тип        │ Статус   │ Дата назначения    │ ID         │");
        System.out.println("├──────────────┼──────────────────┼────────────┼──────────┼────────────────────┼────────────┤");

        for (RoleAssignment ra : assignments) {
            String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
            String type = ra.assignmentType();

            if (ra instanceof TemporaryAssignment) {
                type = "TEMP";
            }

            System.out.printf("│ %-12s │ %-16s │ %-10s │ %-8s │ %-18s │ %-10s │\n",
                    truncate(ra.user().username(), 12),
                    truncate(ra.role().getName(), 16),
                    type,
                    status,
                    ra.metadata().assignedAt(),
                    truncate(ra.assignmentId(), 10));
        }

        System.out.println("└──────────────┴──────────────────┴────────────┴──────────┴────────────────────┴────────────┘");
        System.out.println("Всего назначений: " + assignments.size());
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
            System.out.println("Истекает: " + temp.getExpiresAt());
            System.out.println("Осталось: " + temp.getTimeRemaining());
        }
    }

    private static void addPermissionsToRole(Role role, Scanner scanner, RBACSystem system) {
        System.out.println("\nХотите добавить права к роли? (да/нет)");
        String answer = scanner.nextLine().trim();

        while (answer.equalsIgnoreCase("да")) {
            System.out.println("\nВведите данные права:");
            System.out.print("  Название права (READ, WRITE, DELETE и т.д.): ");
            String permName = scanner.nextLine().trim().toUpperCase();

            System.out.print("  Ресурс (users, roles, reports и т.д.): ");
            String resource = scanner.nextLine().trim().toLowerCase();

            System.out.print("  Описание: ");
            String permDescription = scanner.nextLine().trim();

            try {
                Permission permission = new Permission(permName, resource, permDescription);
                system.getRoleManager().addPermissionToRole(role.getName(), permission);
                System.out.println("Право добавлено!");
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }

            System.out.println("\nДобавить еще одно право? (да/нет)");
            answer = scanner.nextLine().trim();
        }
    }

    private static String truncate(String str, int length) {
        if (str == null) return "";
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

}
