package com.banksystem.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Deadlock Detector - monitors threads for deadlocks using JMX.
 * 
 * Demonstrates:
 * - ThreadMXBean for thread monitoring
 * - Deadlock detection at runtime
 * - Daemon threads for background monitoring
 */
public class DeadlockDetector implements Runnable {

    private final long checkIntervalMs;
    private volatile boolean running = true;

    public DeadlockDetector(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("DeadlockDetector");
        System.out.printf("[DeadlockDetector] Started. Checking every %dms%n", checkIntervalMs);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        while (running) {
            try {
                long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();

                if (deadlockedThreadIds != null) {
                    System.err.println("\n!!! DEADLOCK DETECTED !!!");
                    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);

                    for (ThreadInfo info : threadInfos) {
                        System.err.printf("  Thread: %s (ID: %d)%n", info.getThreadName(), info.getThreadId());
                        System.err.printf("  State: %s%n", info.getThreadState());
                        System.err.printf("  Blocked by: %s%n", info.getLockOwnerName());
                        System.err.printf("  Waiting on: %s%n", info.getLockName());
                        System.err.println("  Stack trace:");
                        for (StackTraceElement ste : info.getStackTrace()) {
                            System.err.printf("    at %s%n", ste);
                        }
                        System.err.println();
                    }
                }

                Thread.sleep(checkIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[DeadlockDetector] Stopped.");
    }

    public void stop() {
        running = false;
    }

    /**
     * One-time deadlock check (can be called from any thread).
     */
    public static boolean checkForDeadlocks() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        return deadlocked != null && deadlocked.length > 0;
    }

    /**
     * Print all active threads (useful for debugging).
     */
    public static void printActiveThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = threadMXBean.dumpAllThreads(false, false);

        System.out.println("\n=== ACTIVE THREADS ===");
        for (ThreadInfo info : threads) {
            System.out.printf("  [%s] State: %s%n", info.getThreadName(), info.getThreadState());
        }
        System.out.printf("  Total: %d threads%n", threads.length);
        System.out.println("=======================\n");
    }
}
