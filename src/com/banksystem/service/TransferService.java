package com.banksystem.service;

import com.banksystem.model.Account;
import com.banksystem.model.Transaction;
import com.banksystem.model.Transaction.Type;
import com.banksystem.model.Transaction.Status;
import com.banksystem.threading.TransactionLogger;

import java.util.concurrent.*;

/**
 * Handles fund transfers between accounts with DEADLOCK PREVENTION.
 * 
 * Deadlock Prevention Strategy: Lock Ordering
 * - Always acquire locks in order of account ID (lower ID first)
 * - This guarantees no circular wait condition → no deadlock
 * 
 * Also demonstrates Future.get(timeout) for transaction timeout handling.
 */
public class TransferService {

    private final BankService bankService;
    private final TransactionLogger transactionLogger;
    private final ExecutorService transferPool;

    public TransferService(BankService bankService, TransactionLogger transactionLogger) {
        this.bankService = bankService;
        this.transactionLogger = transactionLogger;
        // Thread pool for handling concurrent transfers
        this.transferPool = Executors.newFixedThreadPool(4,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("TransferWorker-" + t.getId());
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * Submit a transfer with timeout.
     * Uses Future.get(timeout) to handle slow/stuck transfers.
     */
    public boolean transferWithTimeout(long fromId, long toId, double amount, long timeoutMs) {
        Future<Boolean> future = transferPool.submit(() -> transfer(fromId, toId, amount));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.printf("[%s] TRANSFER TIMEOUT | %d -> %d | $%.2f%n",
                    Thread.currentThread().getName(), fromId, toId, amount);
            logTransaction(fromId, toId, amount, Status.TIMEOUT, "Transfer timed out");
            return false;
        } catch (InterruptedException | ExecutionException e) {
            System.out.printf("[%s] TRANSFER ERROR | %d -> %d | %s%n",
                    Thread.currentThread().getName(), fromId, toId, e.getMessage());
            logTransaction(fromId, toId, amount, Status.FAILED, "Transfer error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Core transfer logic with DEADLOCK PREVENTION via lock ordering.
     * Always locks the account with the smaller ID first.
     */
    public boolean transfer(long fromId, long toId, double amount) {
        if (fromId == toId) return false;

        Account fromAccount = bankService.getAccount(fromId);
        Account toAccount = bankService.getAccount(toId);

        if (fromAccount == null || toAccount == null) {
            logTransaction(fromId, toId, amount, Status.FAILED, "Account not found");
            return false;
        }

        // DEADLOCK PREVENTION: Lock ordering by account ID
        Account firstLock = fromAccount.getAccountId() < toAccount.getAccountId()
                ? fromAccount : toAccount;
        Account secondLock = fromAccount.getAccountId() < toAccount.getAccountId()
                ? toAccount : fromAccount;

        // Acquire locks in consistent order
        firstLock.getLock().writeLock().lock();
        try {
            secondLock.getLock().writeLock().lock();
            try {
                // Critical section: both accounts locked
                if (fromAccount.getBalance() >= amount) {
                    fromAccount.withdraw(amount);
                    toAccount.deposit(amount);

                    String desc = String.format("Transferred $%.2f from %d to %d",
                            amount, fromId, toId);
                    logTransaction(fromId, toId, amount, Status.SUCCESS, desc);
                    System.out.printf("[%s] TRANSFER SUCCESS | %d -> %d | $%.2f%n",
                            Thread.currentThread().getName(), fromId, toId, amount);
                    return true;
                } else {
                    logTransaction(fromId, toId, amount, Status.FAILED, "Insufficient funds");
                    System.out.printf("[%s] TRANSFER FAILED | %d -> %d | Insufficient funds%n",
                            Thread.currentThread().getName(), fromId, toId);
                    return false;
                }
            } finally {
                secondLock.getLock().writeLock().unlock();
            }
        } finally {
            firstLock.getLock().writeLock().unlock();
        }
    }

    public void shutdown() {
        transferPool.shutdown();
        try {
            if (!transferPool.awaitTermination(5, TimeUnit.SECONDS)) {
                transferPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            transferPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void logTransaction(long from, long to, double amount,
                                Status status, String desc) {
        Transaction txn = new Transaction(Type.TRANSFER, from, to, amount, status, desc);
        transactionLogger.log(txn);
    }
}
