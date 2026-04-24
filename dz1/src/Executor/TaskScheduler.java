package Executor;

import interfaces.RoleAssignment;
import managers.AssignmentManager;
import role.TemporaryAssignment;
import util.AuditLog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private final ScheduledExecutorService scheduler;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;

    private ScheduledFuture<?> expiredCheckTask;
    private ScheduledFuture<?> statisticsLogTask;

    private boolean isRunning = false;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TaskScheduler(AssignmentManager assignmentManager, AuditLog auditLog) {
        // Создаём пул с 2 потоками (для двух периодических задач)
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.assignmentManager = assignmentManager;
        this.auditLog = auditLog;
    }

    public void startExpiredCheckTask(long intervalSeconds) {
        if (expiredCheckTask != null && !expiredCheckTask.isDone()) {
            System.out.println("Задача проверки истекших назначений уже запущена");
            return;
        }

        expiredCheckTask = scheduler.scheduleAtFixedRate(
                this::checkAndDeactivateExpiredAssignments,  // задача
                0,                                           // начальная задержка (0 = сразу)
                intervalSeconds,                             // интервал
                TimeUnit.SECONDS                             // единица измерения
        );

        System.out.println("Запущена проверка истекших назначений (каждые " + intervalSeconds + " сек)");
    }

    public void startStatisticsLogTask(long intervalSeconds,
                                       int userCount,
                                       int roleCount,
                                       int assignmentCount) {
        if (statisticsLogTask != null && !statisticsLogTask.isDone()) {
            System.out.println("Задача логирования статистики уже запущена");
            return;
        }

        // Захватываем значения счётчиков для использования в лямбде
        final int users = userCount;
        final int roles = roleCount;
        final int assignments = assignmentCount;

        statisticsLogTask = scheduler.scheduleAtFixedRate(
                () -> logStatistics(users, roles, assignments),
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        System.out.println("Запущено логирование статистики (каждые " + intervalSeconds + " сек)");
    }

    private void checkAndDeactivateExpiredAssignments() {
        try {
            List<RoleAssignment> allAssignments = assignmentManager.findAll();
            int deactivatedCount = 0;

            for (RoleAssignment assignment : allAssignments) {
                // Проверяем только активные временные назначения
                if (assignment.isActive() && assignment instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) assignment;

                    // Если назначение истекло, деактивируем его
                    if (!temp.isActive()) {  // isActive() проверяет дату
                        // Для временных назначений пока нет метода revoke,
                        // поэтому просто логируем
                        deactivatedCount++;
                        auditLog.log(
                                "EXPIRED_ASSIGNMENT",
                                "system",
                                temp.user().username(),
                                "Роль '" + temp.role().getName() + "' истекла"
                        );
                    }
                }
            }

            if (deactivatedCount > 0) {
                System.out.println("[" + LocalDateTime.now().format(FORMATTER) +
                        "] Деактивировано истекших назначений: " + deactivatedCount);
            }

        } catch (Exception e) {
            System.err.println("Ошибка при проверке истекших назначений: " + e.getMessage());
        }
    }

    private void logStatistics(int userCount, int roleCount, int assignmentCount) {
        try {
            int activeCount = assignmentManager.getActiveAssignments().size();
            int expiredCount = assignmentManager.getExpiredAssignments().size();

            String stats = String.format(
                    "Статистика: Пользователей=%d, Ролей=%d, Назначений=%d (активных=%d, истекших=%d)",
                    userCount, roleCount, assignmentCount, activeCount, expiredCount
            );

            auditLog.log("STATS_REPORT", "scheduler", "system", stats);

            System.out.println("[" + LocalDateTime.now().format(FORMATTER) +
                    "] " + stats);

        } catch (Exception e) {
            System.err.println("Ошибка при логировании статистики: " + e.getMessage());
        }
    }

    public void stopAllTasks() {
        if (expiredCheckTask != null) {
            expiredCheckTask.cancel(false);  // false = не прерывать, если выполняется
            expiredCheckTask = null;
            System.out.println("Остановлена проверка истекших назначений");
        }

        if (statisticsLogTask != null) {
            statisticsLogTask.cancel(false);
            statisticsLogTask = null;
            System.out.println("Остановлено логирование статистики");
        }
    }

    public void shutdown() {
        stopAllTasks();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        isRunning = false;
        System.out.println("TaskScheduler остановлен");
    }

    public boolean isRunning() {
        return (expiredCheckTask != null && !expiredCheckTask.isDone()) ||
                (statisticsLogTask != null && !statisticsLogTask.isDone());
    }

    public int getActiveTaskCount() {
        int count = 0;
        if (expiredCheckTask != null && !expiredCheckTask.isDone()) count++;
        if (statisticsLogTask != null && !statisticsLogTask.isDone()) count++;
        return count;
    }
}
