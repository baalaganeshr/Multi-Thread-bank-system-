package com.banksystem.model;

import java.util.concurrent.atomic.AtomicLong;

public class Customer {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final long customerId;
    private final String name;
    private final String email;

    public Customer(String name, String email) {
        this.customerId = ID_GENERATOR.getAndIncrement();
        this.name = name;
        this.email = email;
    }

    public long getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return String.format("Customer[id=%d, name=%s, email=%s]", customerId, name, email);
    }
}
