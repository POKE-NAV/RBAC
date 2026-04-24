package test;

import command.RBACSystem;
import managers.AssignmentManager;
import managers.RoleManager;
import managers.UserManager;
import record.User;
import role.Role;
import record.Permission;
import util.AuditLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.time.format.DateTimeFormatter;

public abstract class BaseThreadTest {

    protected RBACSystem system;
    protected UserManager userManager;
    protected RoleManager roleManager;
    protected AssignmentManager assignmentManager;
    protected AuditLog auditLog;

    protected static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected static final int THREAD_COUNT = 10;
    protected static final int OPERATIONS_PER_THREAD = 20;
    protected static final int TIMEOUT_SECONDS = 30;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        userManager = system.getUserManager();
        roleManager = system.getRoleManager();
        assignmentManager = system.getAssignmentManager();
        auditLog = system.getAuditLog();
    }

    @AfterEach
    void tearDown() {
        if (system != null) {
            // Очистка после каждого теста
            userManager.clear();
            roleManager.clear();
            assignmentManager.clear();
            auditLog.clear();
        }
    }

    protected void initBasicData() {
        try {
            User admin = User.validate("admin", "Admin User", "admin@test.com");
            userManager.add(admin);

            Role baseRole = new Role("BaseRole", "Base role for testing");
            baseRole.addPermissions(new Permission("READ", "users", "Read users"));
            roleManager.add(baseRole);
        } catch (Exception e) {
            // Игнорируем, если уже существуют
        }
    }

    protected void waitForTasks() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}