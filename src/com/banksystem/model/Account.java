package com.banksystem.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe bank account with ReadWriteLock for concurrent reads and exclusive writes.
 * Uses lock ordering (by account ID) in transfers to prevent deadlocks.
 */
public class Account {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);

    private final long accountId;
    private final Customer customer;
    private double balance;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Account(Customer customer, double initialBalance) {
        this.accountId = ID_GENERATOR.getAndIncrement();
        this.customer = customer;
        this.balance = initialBalance;
    }

    public long getAccountId() {
        return accountId;
    }

    public Customer getCustomer() {
        return customer;
    }

    // --- Thread-safe balance read using ReadLock ---
    public double getBalance() {
        lock.readLock().lock();
        try {
            return balance;
        } finally {
            lock.readLock().unlock();
        }
    }

    // --- Thread-safe deposit using WriteLock ---
    public boolean deposit(double amount) {
        if (amount <= 0) return false;
        lock.writeLock().lock();
        try {
            balance += amount;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Thread-safe withdrawal using WriteLock ---
    public boolean withdraw(double amount) {
        if (amount <= 0) return false;
        lock.writeLock().lock();
        try {
            if (balance >= amount) {
                balance -= amount;
                return true;
            }
            return false; // Insufficient funds
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Apply interest (used by ScheduledExecutorService) ---
    public void applyInterest(double rate) {
        lock.writeLock().lock();
        try {
            double interest = balance * rate;
            balance += interest;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Expose lock for transfer service (deadlock-safe lock ordering)
    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return String.format("Account[id=%d, customer=%s, balance=%.2f]",
                accountId, customer.getName(), getBalance());
    }
}
