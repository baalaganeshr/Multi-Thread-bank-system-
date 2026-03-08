package com.banksystem.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe transaction ID generator using AtomicLong.
 * 
 * Demonstrates: AtomicLong for lock-free thread-safe counter.
 * Multiple threads can generate unique IDs simultaneously without synchronization.
 */
public class TransactionIdGenerator {

    private static final AtomicLong counter = new AtomicLong(0);

    /**
     * Generate next unique transaction ID.
     * AtomicLong.getAndIncrement() uses CAS (Compare-And-Swap) internally
     * which is faster than synchronized blocks for simple counters.
     */
    public static long nextId() {
        return counter.getAndIncrement();
    }

    /**
     * Get the current count (how many IDs have been generated).
     */
    public static long getCurrentCount() {
        return counter.get();
    }

    /**
     * Reset counter (for testing purposes).
     */
    public static void reset() {
        counter.set(0);
    }
}
