package test;

import record.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserManager Потокобезопасные тесты")
class UserManagerThreadTest extends BaseThreadTest {

    @Test
    @DisplayName("Параллельное добавление пользователей - уникальные username")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelAddUsers_UniqueUsernames() throws InterruptedException {
        int threadCount = 20;
        int usersPerThread = 10;
        int totalUsers = threadCount * usersPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < usersPerThread; i++) {
                        String username = "test_user_" + threadId + "_" + i;
                        String fullName = "User " + threadId + "_" + i;
                        String email = username + "@test.com";

                        try {
                            User user = User.validate(username, fullName, email);
                            userManager.add(user);
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

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(totalUsers, successCount.get(), "Все пользователи должны быть успешно добавлены");
        assertEquals(0, failureCount.get(), "Не должно быть ошибок");
        assertEquals(totalUsers, userManager.count(), "Количество пользователей должно совпадать");
    }

    @Test
    @DisplayName("Параллельное добавление пользователей - дубликаты username")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelAddUsers_DuplicateUsernames() throws InterruptedException {
        int threadCount = 30;
        String duplicateUsername = "duplicate_user";

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    User user = User.validate(duplicateUsername, "Duplicate User", "dup@test.com");
                    userManager.add(user);
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

        assertEquals(1, successCount.get(), "Только один пользователь должен быть добавлен");
        assertEquals(threadCount - 1, failureCount.get(), "Остальные должны получить ошибку дубликата");
        assertEquals(1, userManager.count(), "В системе должен быть только один пользователь");
    }

    @Test
    @DisplayName("Параллельный поиск пользователей по username")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelFindByUsername() throws InterruptedException {
        // Подготовка данных
        for (int i = 0; i < 50; i++) {
            User user = User.validate("find_user_" + i, "Find User " + i, "find" + i + "@test.com");
            userManager.add(user);
        }

        int threadCount = 20;
        int searchesPerThread = 50;
        int totalSearches = threadCount * searchesPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger foundCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < searchesPerThread; i++) {
                        int id = random.nextInt(50);
                        Optional<User> user = userManager.findByUsername("find_user_" + id);
                        if (user.isPresent()) {
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

        assertEquals(totalSearches, foundCount.get(), "Все поиски должны быть успешными");
    }

    @Test
    @DisplayName("Параллельное обновление пользователей")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelUpdateUsers() throws InterruptedException {
        // Подготовка данных
        for (int i = 0; i < 20; i++) {
            User user = User.validate("update_user_" + i, "Original Name " + i, "update" + i + "@test.com");
            userManager.add(user);
        }

        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    String username = "update_user_" + threadId;
                    String newFullName = "Updated Name " + threadId + "_" + System.nanoTime();
                    String newEmail = "updated" + threadId + "@test.com";

                    userManager.update(username, newFullName, newEmail);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount, successCount.get(), "Все обновления должны быть успешны");

        // Проверяем, что данные обновлены корректно
        for (int i = 0; i < threadCount; i++) {
            Optional<User> user = userManager.findByUsername("update_user_" + i);
            assertTrue(user.isPresent());
            assertTrue(user.get().fullname().startsWith("Updated Name " + i));
        }
    }

    @Test
    @DisplayName("Параллельное удаление пользователей")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelDeleteUsers() throws InterruptedException {
        // Подготовка данных
        for (int i = 0; i < 50; i++) {
            User user = User.validate("delete_user_" + i, "Delete User " + i, "delete" + i + "@test.com");
            userManager.add(user);
        }

        int threadCount = 25;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger deletedCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    User user = userManager.findByUsername("delete_user_" + threadId).orElse(null);
                    if (user != null && userManager.remove(user)) {
                        deletedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount, deletedCount.get(), "Должно быть удалено указанное количество пользователей");
        assertEquals(50 - threadCount, userManager.count(), "Оставшееся количество должно соответствовать");
    }
}