package com.banksystem.service;

import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.model.Transaction;
import com.banksystem.model.Transaction.Type;
import com.banksystem.model.Transaction.Status;
import com.banksystem.threading.TransactionLogger;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core banking service with thread-safe account registry using ConcurrentHashMap.
 * Handles account creation, deposits, and withdrawals.
 */
public class BankService {

    private final ConcurrentHashMap<Long, Account> accounts = new ConcurrentHashMap<>();
    private final TransactionLogger transactionLogger;

    public BankService(TransactionLogger transactionLogger) {
        this.transactionLogger = transactionLogger;
    }

    // --- Create a new account (thread-safe via ConcurrentHashMap) ---
    public Account createAccount(Customer customer, double initialBalance) {
        Account account = new Account(customer, initialBalance);
        accounts.put(account.getAccountId(), account);
        System.out.printf("[%s] Account created: %s%n",
                Thread.currentThread().getName(), account);
        return account;
    }

    // --- Deposit (thread-safe via Account's WriteLock) ---
    public boolean deposit(long accountId, double amount) {
        Account account = accounts.get(accountId);
        if (account == null) {
            logTransaction(Type.DEPOSIT, accountId, accountId, amount,
                    Status.FAILED, "Account not found");
            return false;
        }

        boolean success = account.deposit(amount);
        Status status = success ? Status.SUCCESS : Status.FAILED;
        String desc = success
                ? String.format("Deposited $%.2f. New balance: $%.2f", amount, account.getBalance())
                : "Invalid deposit amount";

        logTransaction(Type.DEPOSIT, accountId, accountId, amount, status, desc);
        System.out.printf("[%s] DEPOSIT %s | Account %d | $%.2f%n",
                Thread.currentThread().getName(), status, accountId, amount);
        return success;
    }

    // --- Withdrawal (thread-safe via Account's WriteLock) ---
    public boolean withdraw(long accountId, double amount) {
        Account account = accounts.get(accountId);
        if (account == null) {
            logTransaction(Type.WITHDRAWAL, accountId, accountId, amount,
                    Status.FAILED, "Account not found");
            return false;
        }

        boolean success = account.withdraw(amount);
        Status status = success ? Status.SUCCESS : Status.FAILED;
        String desc = success
                ? String.format("Withdrew $%.2f. New balance: $%.2f", amount, account.getBalance())
                : "Insufficient funds or invalid amount";

        logTransaction(Type.WITHDRAWAL, accountId, accountId, amount, status, desc);
        System.out.printf("[%s] WITHDRAW %s | Account %d | $%.2f%n",
                Thread.currentThread().getName(), status, accountId, amount);
        return success;
    }

    // --- Get account by ID ---
    public Account getAccount(long accountId) {
        return accounts.get(accountId);
    }

    // --- Get all accounts ---
    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    // --- Log transaction via BlockingQueue (producer side) ---
    private void logTransaction(Type type, long from, long to,
                                double amount, Status status, String desc) {
        Transaction txn = new Transaction(type, from, to, amount, status, desc);
        transactionLogger.log(txn);
    }
}
