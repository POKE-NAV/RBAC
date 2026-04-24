package test;

import command.RBACSystem;
import filters.UserFilters;
import filters.RoleFilters;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Комплексные нагрузочные тесты RBAC")
@Execution(ExecutionMode.CONCURRENT)
class LoadTestJUnit extends BaseThreadTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("Смешанная нагрузка: все операции одновременно")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testMixedLoad() throws InterruptedException {
        int threadCount = 30;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < operationsPerThread; i++) {
                        int operation = random.nextInt(10);

                        try {
                            switch (operation) {
                                case 0: case 1: // Создание пользователя
                                    String username = "mixed_" + threadId + "_" + i + "_" + System.nanoTime();
                                    record.User user = record.User.validate(username, "Mixed User", username + "@test.com");
                                    userManager.add(user);
                                    break;

                                case 2: case 3: // Создание роли
                                    String roleName = "MixedRole_" + threadId + "_" + i;
                                    role.Role role = new role.Role(roleName, "Mixed test role");
                                    roleManager.add(role);
                                    break;

                                case 4: case 5: // Поиск пользователей
                                    userManager.findByFilter(UserFilters.byEmailDomain("@test.com"));
                                    break;

                                case 6: case 7: // Поиск ролей
                                    roleManager.findByFilter(RoleFilters.byNameContains("Mixed"));
                                    break;

                                case 8: case 9: // Статистика
                                    system.generateStatistics();
                                    break;
                            }
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        System.out.println("\n=== Смешанная нагрузка ===");
        System.out.println("Время: " + (endTime - startTime) + " мс");
        System.out.println("Успешно: " + successCount.get());
        System.out.println("Ошибок: " + failureCount.get());
        System.out.println("Пользователей: " + userManager.count());
        System.out.println("Ролей: " + roleManager.count());

        assertTrue(successCount.get() > 0, "Должны быть успешные операции");
        assertTrue(failureCount.get() <= successCount.get() / 2, "Слишком много ошибок");
    }

    @Test
    @DisplayName("Тест на отсутствие Deadlock")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testNoDeadlock() throws InterruptedException {
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        // Чередуем чтение и запись
                        if (i % 2 == 0) {
                            userManager.findAll();
                            roleManager.findAll();
                            assignmentManager.findAll();
                        } else {
                            String username = "deadlock_" + "_" + i;
                            try {
                                record.User user = record.User.validate(username, "Test", username + "@test.com");
                                userManager.add(user);
                            } catch (Exception e) {
                                // Игнорируем дубликаты
                            }
                        }
                    }
                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Все потоки должны завершиться (нет deadlock)");
        assertEquals(threadCount, completedCount.get(), "Все потоки должны успешно выполниться");
    }

    @Test
    @DisplayName("Стабильность при длительной нагрузке")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testStabilityUnderLoad() throws InterruptedException {
        int threadCount = 15;
        int iterations = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalOperations = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        // Выполняем различные операции в цикле
                        if (i % 3 == 0) {
                            userManager.findAll();
                        } else if (i % 3 == 1) {
                            roleManager.findAll();
                        } else {
                            assignmentManager.findAll();
                        }
                        totalOperations.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(25, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Все операции должны завершиться");
        assertEquals(threadCount * iterations, totalOperations.get(), "Все операции должны быть выполнены");

        // Проверяем, что система осталась в стабильном состоянии
        assertNotNull(userManager.findAll());
        assertNotNull(roleManager.findAll());
        assertNotNull(assignmentManager.findAll());
        assertTrue(userManager.count() >= 0);
        assertTrue(roleManager.count() >= 0);
        assertTrue(assignmentManager.count() >= 0);
    }
}