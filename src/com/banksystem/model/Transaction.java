package com.banksystem.model;

import com.banksystem.util.TransactionIdGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER, INTEREST
    }

    public enum Status {
        SUCCESS, FAILED, TIMEOUT, PENDING
    }

    private final long transactionId;
    private final Type type;
    private final long fromAccountId;
    private final long toAccountId;
    private final double amount;
    private final Status status;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(Type type, long fromAccountId, long toAccountId,
                       double amount, Status status, String description) {
        this.transactionId = TransactionIdGenerator.nextId();
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public long getTransactionId() { return transactionId; }
    public Type getType() { return type; }
    public long getFromAccountId() { return fromAccountId; }
    public long getToAccountId() { return toAccountId; }
    public double getAmount() { return amount; }
    public Status getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[TXN#%d | %s | %s | From:%d -> To:%d | $%.2f | %s | %s]",
                transactionId, timestamp.format(fmt), type, fromAccountId,
                toAccountId, amount, status, description);
    }
}
