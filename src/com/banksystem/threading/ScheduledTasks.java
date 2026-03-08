package com.banksystem.threading;

import java.util.concurrent.*;

/**
 * Manages scheduled tasks for the banking system.
 * 
 * Demonstrates:
 * - ScheduledExecutorService for recurring tasks
 * - CyclicBarrier for synchronized batch operations
 * - Periodic reporting and monitoring
 */
public class ScheduledTasks {

    private final ScheduledExecutorService scheduler;

    public ScheduledTasks() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("ScheduledTask-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Schedule periodic balance reporting for all accounts.
     */
    public void scheduleBalanceReport(Runnable reportTask, long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                reportTask.run();
            } catch (Exception e) {
                System.err.printf("[ScheduledTasks] Report error: %s%n", e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        System.out.printf("[ScheduledTasks] Balance report scheduled every %ds%n", intervalSeconds);
    }

    /**
     * Schedule a one-time delayed task.
     * Demonstrates: schedule() with delay
     */
    public ScheduledFuture<?> scheduleOneTime(Runnable task, long delaySeconds) {
        return scheduler.schedule(task, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Create a CyclicBarrier for synchronized batch processing.
     * All participating threads wait until everyone reaches the barrier.
     * 
     * Demonstrates: CyclicBarrier for coordinated thread execution
     */
    public CyclicBarrier createBatchBarrier(int parties, Runnable barrierAction) {
        return new CyclicBarrier(parties, barrierAction);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
