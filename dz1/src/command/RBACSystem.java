package command;

import interfaces.RoleAssignment;
import managers.AssignmentManager;
import managers.RoleManager;
import managers.UserManager;
import record.AssignmentMetadata;
import record.Permission;
import record.User;
import role.PermanentAssignment;
import role.Role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager();

        assignmentManager.setUserManager(userManager);
        assignmentManager.setRoleManager(roleManager);
        roleManager.setAssignmentManager(assignmentManager);
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        System.out.println("Инициализация системы RBAC");

        createDefaultPermissions();

        createDefaultRoles();

        createAdminUser();

        assignAdminRole();

        System.out.println("Инициализация завершена!");
        System.out.println(generateStatistics());

    }

    private void createDefaultPermissions() {
        System.out.println("  Создание прав доступа...");

        // Права для пользователей
        Permission readUsers = new Permission("READ", "users", "Просмотр пользователей");
        Permission writeUsers = new Permission("WRITE", "users", "Создание/редактирование пользователей");
        Permission deleteUsers = new Permission("DELETE", "users", "Удаление пользователей");

        // Права для ролей
        Permission readRoles = new Permission("READ", "roles", "Просмотр ролей");
        Permission writeRoles = new Permission("WRITE", "roles", "Создание/редактирование ролей");
        Permission deleteRoles = new Permission("DELETE", "roles", "Удаление ролей");

        // Права для назначений
        Permission readAssignments = new Permission("READ", "assignments", "Просмотр назначений");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Создание/редактирование назначений");
        Permission deleteAssignments = new Permission("DELETE", "assignments", "Удаление назначений");

        // Права для отчетов
        Permission viewReports = new Permission("VIEW", "reports", "Просмотр отчетов");

        System.out.println("  Создано 10 прав доступа");
    }

    private void createDefaultRoles() {
        System.out.println("  Создание базовых ролей...");

        // РОЛЬ 1: Admin - полный доступ ко всем ресурсам
        Role adminRole = new Role("Admin", "Полный доступ к системе");
        adminRole.addPermissions(new Permission("READ", "users", "Просмотр пользователей"));
        adminRole.addPermissions(new Permission("WRITE", "users", "Создание/редактирование пользователей"));
        adminRole.addPermissions(new Permission("DELETE", "users", "Удаление пользователей"));
        adminRole.addPermissions(new Permission("READ", "roles", "Просмотр ролей"));
        adminRole.addPermissions(new Permission("WRITE", "roles", "Создание/редактирование ролей"));
        adminRole.addPermissions(new Permission("DELETE", "roles", "Удаление ролей"));
        adminRole.addPermissions(new Permission("READ", "assignments", "Просмотр назначений"));
        adminRole.addPermissions(new Permission("WRITE", "assignments", "Создание/редактирование назначений"));
        adminRole.addPermissions(new Permission("DELETE", "assignments", "Удаление назначений"));
        adminRole.addPermissions(new Permission("VIEW", "reports", "Просмотр отчетов"));

        // РОЛЬ 2: Manager - может управлять пользователями и смотреть отчеты
        Role managerRole = new Role("Manager", "Управление пользователями и просмотр отчетов");
        managerRole.addPermissions(new Permission("READ", "users", "Просмотр пользователей"));
        managerRole.addPermissions(new Permission("WRITE", "users", "Редактирование пользователей"));
        managerRole.addPermissions(new Permission("READ", "roles", "Просмотр ролей"));
        managerRole.addPermissions(new Permission("READ", "assignments", "Просмотр назначений"));
        managerRole.addPermissions(new Permission("VIEW", "reports", "Просмотр отчетов"));

        // РОЛЬ 3: Viewer - только просмотр информации
        Role viewerRole = new Role("Viewer", "Только просмотр");
        viewerRole.addPermissions(new Permission("READ", "users", "Просмотр пользователей"));
        viewerRole.addPermissions(new Permission("READ", "roles", "Просмотр ролей"));
        viewerRole.addPermissions(new Permission("READ", "assignments", "Просмотр назначений"));
        viewerRole.addPermissions(new Permission("VIEW", "reports", "Просмотр отчетов"));

        // Добавляем роли в менеджер
        try {
            roleManager.add(adminRole);
            roleManager.add(managerRole);
            roleManager.add(viewerRole);
            System.out.println("  Создано 3 роли: Admin, Manager, Viewer");
        } catch (IllegalStateException e) {
            System.out.println("  Роли уже существуют: " + e.getMessage());
        }
    }

    private void createAdminUser() {
        System.out.println("  Создание тестового администратора...");

        try {
            User admin = User.validate("admin", "System Administrator", "admin@system.com");
            userManager.add(admin);
            setCurrentUser("admin");
            System.out.println("  ✅ Администратор создан: admin");
        } catch (IllegalStateException e) {
            System.out.println("  ⚠️ Администратор уже существует");
            setCurrentUser("admin");
        }
    }

    private void assignAdminRole() {
        System.out.println("  Назначение роли Admin администратору...");

        User admin = userManager.findByUsername("admin").orElse(null);
        Role adminRole = roleManager.findByName("Admin").orElse(null);

        if (admin == null || adminRole == null) {
            System.out.println("   Не удалось найти администратора или роль Admin");
            return;
        }

        // Проверяем, есть ли уже назначение
        if (assignmentManager.hasActiveAssignment("admin", "Admin")) {
            System.out.println("   Роль Admin уже назначена администратору");
            return;
        }

        // Создаем метаданные для назначения
        AssignmentMetadata metadata = new AssignmentMetadata(
                "system",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "Автоматическое назначение при инициализации системы"
        );

        // Создаем постоянное назначение
        RoleAssignment assignment = new PermanentAssignment(admin, adminRole, metadata);

        try {
            assignmentManager.add(assignment);
            System.out.println("   Роль Admin назначена администратору");
        } catch (IllegalStateException e) {
            System.out.println("   Ошибка при назначении: " + e.getMessage());
        }
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nСТАТИСТИКА СИСТЕМЫ \n");
        sb.append(String.format("Пользователей: %d\n", userManager.count()));
        sb.append(String.format("Ролей: %d\n", roleManager.count()));
        sb.append(String.format("Назначений: %d\n", assignmentManager.count()));

        // Дополнительная статистика
        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();

        sb.append(String.format("Активных назначений: %d\n", activeAssignments));
        sb.append(String.format("Неактивных/истекших: %d\n", expiredAssignments));

        // Если есть текущий пользователь
        if (currentUser != null) {
            sb.append(String.format("Текущий пользователь: %s\n", currentUser));
        }

        sb.append("_____________________________________________________\n");

        return sb.toString();
    }

}
