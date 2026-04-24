package Executor;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor {

    private final ExecutorService executorService;
    private volatile  boolean isShutdown = false;

    public BackgroundExecutor(int threadCount) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public BackgroundExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public Future<?> submit(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor уже остановлен");
        }
        return executorService.submit(task);
    }

    public void submitWithCallback(Runnable task, Runnable callback) {
        submit(() -> {
            try {
                task.run();
                if (callback != null) {
                    callback.run();
                }
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении фоновой задачи: " + e.getMessage());
            }
        });
    }

    public boolean hasActiveTasks() {
        return !executorService.isTerminated() && !executorService.isShutdown();
    }

    public void shutdownGracefully(int timeoutSeconds) {
        isShutdown = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownNow() {
        isShutdown = true;
        executorService.shutdownNow();
    }

    public int getPoolSize() {
        return ((java.util.concurrent.ThreadPoolExecutor) executorService).getPoolSize();
    }

    public int getActiveCount() {
        return ((java.util.concurrent.ThreadPoolExecutor) executorService).getActiveCount();
    }
}
