package com.banksystem.threading;

import com.banksystem.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Transaction Logger using Producer-Consumer pattern with BlockingQueue.
 * 
 * - Producers: BankService, TransferService, InterestService (add transactions)
 * - Consumer: This logger thread (reads and stores transactions)
 * 
 * Demonstrates: BlockingQueue, Producer-Consumer pattern, daemon threads
 */
public class TransactionLogger implements Runnable {

    private final BlockingQueue<Transaction> transactionQueue;
    private final List<Transaction> transactionHistory;
    private volatile boolean running = true;

    public TransactionLogger() {
        this.transactionQueue = new LinkedBlockingQueue<>(1000);
        this.transactionHistory = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Producer method - called by service threads to log transactions.
     * Non-blocking: uses offer() to avoid blocking the service thread.
     */
    public void log(Transaction transaction) {
        if (!transactionQueue.offer(transaction)) {
            System.err.printf("[TransactionLogger] Queue full! Dropped: %s%n", transaction);
        }
    }

    /**
     * Consumer loop - runs on a separate thread.
     * Uses poll() with timeout to allow graceful shutdown.
     */
    @Override
    public void run() {
        System.out.printf("[%s] Transaction Logger started. Waiting for transactions...%n",
                Thread.currentThread().getName());

        while (running || !transactionQueue.isEmpty()) {
            try {
                Transaction txn = transactionQueue.poll(500, TimeUnit.MILLISECONDS);
                if (txn != null) {
                    transactionHistory.add(txn);
                    System.out.printf("[%s] LOGGED: %s%n",
                            Thread.currentThread().getName(), txn);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Drain remaining transactions
        List<Transaction> remaining = new ArrayList<>();
        transactionQueue.drainTo(remaining);
        transactionHistory.addAll(remaining);
        remaining.forEach(txn -> System.out.printf("[Logger] DRAIN: %s%n", txn));

        System.out.printf("[%s] Transaction Logger stopped. Total logged: %d%n",
                Thread.currentThread().getName(), transactionHistory.size());
    }

    public void stop() {
        running = false;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(new ArrayList<>(transactionHistory));
    }

    public int getPendingCount() {
        return transactionQueue.size();
    }

    public int getTotalLogged() {
        return transactionHistory.size();
    }
}
