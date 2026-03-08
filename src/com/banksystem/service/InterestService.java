package com.banksystem.service;

import com.banksystem.model.Account;
import com.banksystem.model.Transaction;
import com.banksystem.model.Transaction.Type;
import com.banksystem.model.Transaction.Status;
import com.banksystem.threading.TransactionLogger;

import java.util.Collection;
import java.util.concurrent.*;

/**
 * Scheduled interest calculation using ScheduledExecutorService.
 * Applies interest to all accounts at fixed intervals.
 * Demonstrates: ScheduledExecutorService, CyclicBarrier
 */
public class InterestService {

    private final BankService bankService;
    private final TransactionLogger transactionLogger;
    private final ScheduledExecutorService scheduler;
    private final double annualRate;

    public InterestService(BankService bankService, TransactionLogger transactionLogger,
                           double annualRate) {
        this.bankService = bankService;
        this.transactionLogger = transactionLogger;
        this.annualRate = annualRate;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("InterestScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start periodic interest calculation.
     * In simulation: runs every few seconds (represents monthly/yearly in real system).
     */
    public void startScheduledInterest(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::applyInterestToAll,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        System.out.printf("[InterestService] Scheduled interest at %.2f%% every %ds%n",
                annualRate * 100, intervalSeconds);
    }

    /**
     * Apply interest to all accounts.
     * Each account's lock ensures thread-safety during balance update.
     */
    private void applyInterestToAll() {
        Collection<Account> allAccounts = bankService.getAllAccounts();
        System.out.printf("%n[%s] === APPLYING INTEREST (%.2f%%) TO %d ACCOUNTS ===%n",
                Thread.currentThread().getName(), annualRate * 100, allAccounts.size());

        // Use CountDownLatch to wait for all interest calculations
        CountDownLatch latch = new CountDownLatch(allAccounts.size());
        ExecutorService workers = Executors.newFixedThreadPool(
                Math.min(allAccounts.size(), 4));

        for (Account account : allAccounts) {
            workers.submit(() -> {
                try {
                    double beforeBalance = account.getBalance();
                    account.applyInterest(annualRate);
                    double afterBalance = account.getBalance();
                    double interest = afterBalance - beforeBalance;

                    String desc = String.format("Interest $%.2f applied (%.2f%%). Balance: $%.2f -> $%.2f",
                            interest, annualRate * 100, beforeBalance, afterBalance);

                    Transaction txn = new Transaction(Type.INTEREST,
                            account.getAccountId(), account.getAccountId(),
                            interest, Status.SUCCESS, desc);
                    transactionLogger.log(txn);

                    System.out.printf("  [%s] Account %d: +$%.2f interest%n",
                            Thread.currentThread().getName(),
                            account.getAccountId(), interest);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        workers.shutdown();
        System.out.printf("[%s] === INTEREST CALCULATION COMPLETE ===%n",
                Thread.currentThread().getName());
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
