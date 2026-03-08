package com.banksystem;

import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.service.BankService;
import com.banksystem.service.InterestService;
import com.banksystem.service.TransferService;
import com.banksystem.threading.ScheduledTasks;
import com.banksystem.threading.TransactionLogger;
import com.banksystem.threading.TransactionWorker;
import com.banksystem.util.DeadlockDetector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 *   MULTI-THREADED BANK TRANSACTION SYSTEM - SIMULATION
 * ============================================================
 * 
 * This simulation demonstrates the following concurrency concepts:
 * 
 *  1. Thread & Runnable          → TransactionWorker, TransactionLogger
 *  2. synchronized               → Account balance operations
 *  3. ReadWriteLock              → Account read/write separation
 *  4. ReentrantLock              → Account locks for transfers
 *  5. Lock Ordering              → Deadlock prevention in TransferService
 *  6. AtomicLong                 → TransactionIdGenerator, Account IDs
 *  7. volatile                   → Logger/Detector stop flags
 *  8. BlockingQueue              → Producer-Consumer transaction logging
 *  9. ExecutorService            → Thread pools for workers & transfers
 * 10. ScheduledExecutorService   → Periodic interest calculation
 * 11. Future.get(timeout)        → Transfer timeout handling
 * 12. CountDownLatch             → Wait for all workers to finish
 * 13. ConcurrentHashMap          → Thread-safe account registry
 * 14. Deadlock Detection         → JMX ThreadMXBean monitoring
 */
public class Main {

    private static final int NUM_WORKERS = 5;
    private static final int TRANSACTIONS_PER_WORKER = 8;
    private static final double INTEREST_RATE = 0.02; // 2%

    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   MULTI-THREADED BANK TRANSACTION SYSTEM            ║");
        System.out.println("║   Demonstrating Java Concurrency Concepts           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // ─── STEP 1: Start Transaction Logger (Consumer Thread) ───
        System.out.println(">>> STEP 1: Starting Transaction Logger (BlockingQueue Consumer)...");
        TransactionLogger logger = new TransactionLogger();
        Thread loggerThread = new Thread(logger, "TxnLogger");
        loggerThread.setDaemon(true);
        loggerThread.start();

        // ─── STEP 2: Start Deadlock Detector (Background Monitor) ───
        System.out.println(">>> STEP 2: Starting Deadlock Detector (JMX Monitor)...");
        DeadlockDetector deadlockDetector = new DeadlockDetector(2000);
        Thread detectorThread = new Thread(deadlockDetector, "DeadlockDetector");
        detectorThread.setDaemon(true);
        detectorThread.start();

        // ─── STEP 3: Initialize Services ───
        System.out.println(">>> STEP 3: Initializing Bank Services...");
        BankService bankService = new BankService(logger);
        TransferService transferService = new TransferService(bankService, logger);
        InterestService interestService = new InterestService(bankService, logger, INTEREST_RATE);

        // ─── STEP 4: Create Accounts ───
        System.out.println("\n>>> STEP 4: Creating Accounts (AtomicLong ID generation)...");
        Account acc1 = bankService.createAccount(new Customer("Alice Johnson", "alice@bank.com"), 5000.00);
        Account acc2 = bankService.createAccount(new Customer("Bob Smith", "bob@bank.com"), 3000.00);
        Account acc3 = bankService.createAccount(new Customer("Charlie Brown", "charlie@bank.com"), 7500.00);
        Account acc4 = bankService.createAccount(new Customer("Diana Prince", "diana@bank.com"), 2000.00);
        Account acc5 = bankService.createAccount(new Customer("Edward Norton", "edward@bank.com"), 10000.00);

        long[] accountIds = {
                acc1.getAccountId(), acc2.getAccountId(), acc3.getAccountId(),
                acc4.getAccountId(), acc5.getAccountId()
        };

        // ─── STEP 5: Print Initial Balances ───
        System.out.println("\n>>> STEP 5: Initial Account Balances:");
        printBalances(bankService, accountIds);

        // ─── STEP 6: Start Scheduled Interest (ScheduledExecutorService) ───
        System.out.println("\n>>> STEP 6: Starting Scheduled Interest Service...");
        interestService.startScheduledInterest(10); // Every 10 seconds

        // ─── STEP 7: Start Scheduled Balance Reports ───
        ScheduledTasks scheduledTasks = new ScheduledTasks();
        scheduledTasks.scheduleBalanceReport(
                () -> printBalances(bankService, accountIds), 8);

        // ─── STEP 8: Launch Worker Threads (ExecutorService + CountDownLatch) ───
        System.out.println("\n>>> STEP 8: Launching " + NUM_WORKERS + " Transaction Workers...");
        System.out.println("    Each worker will process " + TRANSACTIONS_PER_WORKER + " random transactions.");
        System.out.println("    (deposits, withdrawals, transfers happening concurrently)\n");

        ExecutorService workerPool = Executors.newFixedThreadPool(NUM_WORKERS);
        CountDownLatch allWorkersDone = new CountDownLatch(NUM_WORKERS);

        for (int i = 1; i <= NUM_WORKERS; i++) {
            final int workerId = i;
            workerPool.submit(() -> {
                try {
                    new TransactionWorker(
                            "Worker-" + workerId,
                            bankService, transferService,
                            accountIds, TRANSACTIONS_PER_WORKER
                    ).run();
                } finally {
                    allWorkersDone.countDown();
                }
            });
        }

        // ─── STEP 9: Wait for All Workers (CountDownLatch) ───
        System.out.println(">>> STEP 9: Waiting for all workers to complete (CountDownLatch)...\n");
        allWorkersDone.await(60, TimeUnit.SECONDS);

        // ─── STEP 10: Print Active Threads ───
        System.out.println("\n>>> STEP 10: Active Thread Report:");
        DeadlockDetector.printActiveThreads();

        // ─── STEP 11: Check for Deadlocks ───
        System.out.println(">>> STEP 11: Final Deadlock Check:");
        if (DeadlockDetector.checkForDeadlocks()) {
            System.err.println("!!! DEADLOCKS DETECTED !!!");
        } else {
            System.out.println("    No deadlocks detected. Lock ordering strategy working correctly.");
        }

        // ─── STEP 12: Final Balances ───
        Thread.sleep(2000); // Let logger catch up
        System.out.println("\n>>> STEP 12: Final Account Balances:");
        printBalances(bankService, accountIds);

        // ─── STEP 13: Transaction Summary ───
        System.out.println("\n>>> STEP 13: Transaction Summary:");
        System.out.printf("    Total transactions logged: %d%n", logger.getTotalLogged());
        System.out.printf("    Pending in queue: %d%n", logger.getPendingCount());

        // ─── STEP 14: Graceful Shutdown ───
        System.out.println("\n>>> STEP 14: Graceful Shutdown...");
        workerPool.shutdown();
        workerPool.awaitTermination(10, TimeUnit.SECONDS);
        transferService.shutdown();
        interestService.shutdown();
        scheduledTasks.shutdown();
        deadlockDetector.stop();
        logger.stop();
        Thread.sleep(1000); // Let logger drain

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║   SIMULATION COMPLETE                                ║");
        System.out.println("║                                                      ║");
        System.out.println("║   Concurrency Concepts Demonstrated:                 ║");
        System.out.println("║   ✓ Thread, Runnable                                ║");
        System.out.println("║   ✓ synchronized, volatile                          ║");
        System.out.println("║   ✓ ReadWriteLock, ReentrantLock                    ║");
        System.out.println("║   ✓ AtomicLong (lock-free counters)                 ║");
        System.out.println("║   ✓ BlockingQueue (Producer-Consumer)               ║");
        System.out.println("║   ✓ ExecutorService, ThreadPoolExecutor             ║");
        System.out.println("║   ✓ ScheduledExecutorService                        ║");
        System.out.println("║   ✓ Future.get(timeout)                             ║");
        System.out.println("║   ✓ CountDownLatch                                  ║");
        System.out.println("║   ✓ ConcurrentHashMap                               ║");
        System.out.println("║   ✓ Deadlock Prevention (Lock Ordering)             ║");
        System.out.println("║   ✓ Deadlock Detection (JMX ThreadMXBean)           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }

    private static void printBalances(BankService bankService, long[] accountIds) {
        System.out.println("    ┌──────────┬─────────────────┬──────────────┐");
        System.out.println("    │ Acct ID  │ Customer        │ Balance      │");
        System.out.println("    ├──────────┼─────────────────┼──────────────┤");
        for (long id : accountIds) {
            Account acc = bankService.getAccount(id);
            if (acc != null) {
                System.out.printf("    │ %-8d │ %-15s │ $%10.2f │%n",
                        acc.getAccountId(), acc.getCustomer().getName(), acc.getBalance());
            }
        }
        System.out.println("    └──────────┴─────────────────┴──────────────┘");
    }
}
