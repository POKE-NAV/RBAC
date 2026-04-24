package test;

import interfaces.RoleAssignment;
import record.User;
import record.AssignmentMetadata;
import role.PermanentAssignment;
import role.Role;
import role.TemporaryAssignment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssignmentManager Потокобезопасные тесты")
class AssignmentManagerThreadTest extends BaseThreadTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUpAssignmentData() {
        // Создаём дополнительных пользователей и роли для тестов
        for (int i = 0; i < 20; i++) {
            try {
                User user = User.validate("assign_user_" + i, "User " + i, "user" + i + "@test.com");
                userManager.add(user);

                Role role = new Role("assign_role_" + i, "Role " + i);
                roleManager.add(role);
            } catch (Exception e) {
                // Игнорируем дубликаты
            }
        }
    }

    @Test
    @DisplayName("Параллельное создание назначений")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testParallelAssignmentCreation() throws InterruptedException {
        int threadCount = 10;
        int assignmentsPerThread = 15;
        int totalAssignments = threadCount * assignmentsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < assignmentsPerThread; i++) {
                        int userId = random.nextInt(20);
                        int roleId = random.nextInt(20);

                        User user = userManager.findByUsername("assign_user_" + userId).orElse(null);
                        Role role = roleManager.findByName("assign_role_" + roleId).orElse(null);

                        if (user != null && role != null) {
                            // Проверяем, нет ли уже назначения
                            if (!assignmentManager.hasActiveAssignment(user.username(), role.getName())) {
                                AssignmentMetadata metadata = new AssignmentMetadata(
                                        "test",
                                        LocalDateTime.now().format(FORMATTER),
                                        "Test assignment"
                                );

                                RoleAssignment assignment = new PermanentAssignment(user, role, metadata);
                                assignmentManager.add(assignment);
                                successCount.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Должны быть созданы назначения");
        System.out.println("Создано назначений: " + successCount.get());
    }

    @Test
    @DisplayName("Параллельное создание назначений - предотвращение дубликатов")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testParallelAssignmentPreventsDuplicates() throws InterruptedException {
        // Берём одного пользователя и одну роль
        User targetUser = userManager.findByUsername("assign_user_0").orElseThrow();
        Role targetRole = roleManager.findByName("assign_role_0").orElseThrow();

        int threadCount = 30;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    AssignmentMetadata metadata = new AssignmentMetadata(
                            "test",
                            LocalDateTime.now().format(FORMATTER),
                            "Duplicate test"
                    );

                    RoleAssignment assignment = new PermanentAssignment(targetUser, targetRole, metadata);
                    assignmentManager.add(assignment);
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

        assertEquals(1, successCount.get(), "Только одно назначение должно быть создано");
        assertEquals(threadCount - 1, failureCount.get(), "Остальные должны получить ошибку дубликата");
    }

    @Test
    @DisplayName("Параллельный поиск назначений по пользователю")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelFindByUser() throws InterruptedException {
        // Создаём несколько назначений
        for (int i = 0; i < 10; i++) {
            User user = userManager.findByUsername("assign_user_" + i).orElseThrow();
            Role role = roleManager.findByName("assign_role_" + i).orElseThrow();

            AssignmentMetadata metadata = new AssignmentMetadata(
                    "test",
                    LocalDateTime.now().format(FORMATTER),
                    "Search test"
            );
            RoleAssignment assignment = new PermanentAssignment(user, role, metadata);
            assignmentManager.add(assignment);
        }

        int threadCount = 10;
        int searchesPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger foundCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < searchesPerThread; i++) {
                        int id = random.nextInt(10);
                        User user = userManager.findByUsername("assign_user_" + id).orElse(null);
                        if (user != null) {
                            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
                            foundCount.addAndGet(assignments.size());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(foundCount.get() > 0);
    }
}