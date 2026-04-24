package test;

import role.Role;
import record.Permission;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import command.RBACSystem;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleManager Потокобезопасные тесты")
class RoleManagerThreadTest extends BaseThreadTest {

    @BeforeEach
    @Override
    void setUp() {
        system = new RBACSystem();
        userManager = system.getUserManager();
        roleManager = system.getRoleManager();
        assignmentManager = system.getAssignmentManager();
        auditLog = system.getAuditLog();

    }

    @Test
    @DisplayName("Параллельное добавление ролей - уникальные имена")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelAddRoles_UniqueNames() throws InterruptedException {
        // Очищаем хранилище перед тестом (на случай, если там есть что-то от других тестов)
        roleManager.clear();

        int threadCount = 10;
        int rolesPerThread = 10;
        int totalRoles = threadCount * rolesPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < rolesPerThread; i++) {
                        String roleName = "test_role_" + threadId + "_" + i;
                        Role role = new Role(roleName, "Test role " + threadId + "_" + i);
                        roleManager.add(role);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Все потоки должны завершиться");
        assertEquals(totalRoles, successCount.get(), "Все добавления должны быть успешными");
        assertEquals(totalRoles, roleManager.count(), "Количество ролей в менеджере должно совпадать с количеством добавленных");
    }

    @Test
    @DisplayName("Параллельное добавление ролей - дубликаты имен")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelAddRoles_DuplicateNames() throws InterruptedException {
        // Очищаем хранилище перед тестом
        roleManager.clear();

        int threadCount = 20;
        String duplicateRoleName = "duplicate_role";

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    Role role = new Role(duplicateRoleName, "Duplicate role");
                    roleManager.add(role);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Только одна роль должна быть добавлена");
        assertEquals(threadCount - 1, failureCount.get(), "Остальные должны получить ошибку дубликата");
        assertEquals(1, roleManager.count(), "В менеджере должна быть только одна роль");
    }

    @Test
    @DisplayName("Параллельное добавление прав к роли")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelAddPermissionsToRole() throws InterruptedException {
        // Очищаем и создаём базовую роль
        roleManager.clear();

        Role targetRole = new Role("PermRole", "Role for permission test");
        roleManager.add(targetRole);

        int threadCount = 20;
        int permissionsPerThread = 5;
        int expectedPermissions = threadCount * permissionsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < permissionsPerThread; i++) {
                        String permName = "PERM_" + threadId + "_" + i;
                        String resource = "resource_" + threadId;
                        Permission perm = new Permission(permName, resource, "Test permission");
                        roleManager.addPermissionToRole("PermRole", perm);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(expectedPermissions, successCount.get(), "Все права должны быть успешно добавлены");

        Role role = roleManager.findByName("PermRole").orElseThrow();
        assertEquals(expectedPermissions, role.getPermissions().size(),
                "Количество прав в роли должно соответствовать количеству добавленных");
    }

    @Test
    @DisplayName("Параллельный поиск ролей по имени")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelFindByName() throws InterruptedException {
        // Очищаем и создаём тестовые данные
        roleManager.clear();

        int testRoleCount = 30;
        for (int i = 0; i < testRoleCount; i++) {
            Role role = new Role("find_role_" + i, "Role " + i);
            roleManager.add(role);
        }

        int threadCount = 15;
        int searchesPerThread = 50;
        int expectedSearches = threadCount * searchesPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger foundCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < searchesPerThread; i++) {
                        int id = random.nextInt(testRoleCount);
                        Optional<Role> role = roleManager.findByName("find_role_" + id);
                        if (role.isPresent()) {
                            foundCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(expectedSearches, foundCount.get(), "Все поиски должны быть успешными");
    }

    @Test
    @DisplayName("Параллельное удаление ролей")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelDeleteRoles() throws InterruptedException {
        // Очищаем и создаём тестовые данные
        roleManager.clear();

        int testRoleCount = 50;
        for (int i = 0; i < testRoleCount; i++) {
            Role role = new Role("delete_role_" + i, "Delete Role " + i);
            roleManager.add(role);
        }

        int threadCount = 25;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger deletedCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Optional<Role> role = roleManager.findByName("delete_role_" + threadId);
                    if (role.isPresent() && roleManager.remove(role.get())) {
                        deletedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount, deletedCount.get(), "Должно быть удалено указанное количество ролей");
        assertEquals(testRoleCount - threadCount, roleManager.count(),
                "Оставшееся количество ролей должно соответствовать");
    }
}