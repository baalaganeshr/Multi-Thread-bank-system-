package com.banksystem.threading;

import com.banksystem.service.BankService;
import com.banksystem.service.TransferService;

import java.util.Random;

/**
 * Transaction Worker - simulates a bank teller/ATM processing random transactions.
 * Each worker runs on its own thread via ExecutorService.
 * 
 * Demonstrates: Runnable, Thread pools, concurrent access to shared services
 */
public class TransactionWorker implements Runnable {

    private final String workerName;
    private final BankService bankService;
    private final TransferService transferService;
    private final long[] accountIds;
    private final int transactionCount;
    private final Random random = new Random();

    public TransactionWorker(String workerName, BankService bankService,
                             TransferService transferService,
                             long[] accountIds, int transactionCount) {
        this.workerName = workerName;
        this.bankService = bankService;
        this.transferService = transferService;
        this.accountIds = accountIds;
        this.transactionCount = transactionCount;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(workerName);
        System.out.printf("%n[%s] Worker started. Will process %d transactions.%n",
                workerName, transactionCount);

        for (int i = 0; i < transactionCount; i++) {
            try {
                int operation = random.nextInt(3); // 0=deposit, 1=withdraw, 2=transfer
                long accountId = accountIds[random.nextInt(accountIds.length)];
                double amount = Math.round((random.nextDouble() * 500 + 10) * 100.0) / 100.0;

                switch (operation) {
                    case 0: // Deposit
                        bankService.deposit(accountId, amount);
                        break;

                    case 1: // Withdrawal
                        bankService.withdraw(accountId, amount);
                        break;

                    case 2: // Transfer
                        long toAccountId;
                        do {
                            toAccountId = accountIds[random.nextInt(accountIds.length)];
                        } while (toAccountId == accountId);

                        transferService.transferWithTimeout(accountId, toAccountId,
                                amount, 3000);
                        break;
                }

                // Simulate processing delay
                Thread.sleep(random.nextInt(200) + 50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Worker interrupted.%n", workerName);
                break;
            }
        }

        System.out.printf("[%s] Worker completed %d transactions.%n",
                workerName, transactionCount);
    }
}
