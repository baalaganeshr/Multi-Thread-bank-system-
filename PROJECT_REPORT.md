<div align="center">

# PROJECT REPORT

### Multi-Threaded Bank Transaction System

---

**A Project Report Submitted in Partial Fulfillment of the Requirements**

---

</div>

## PROJECT DETAILS

| | |
|---|---|
| **Project Title** | Multi-Threaded Bank Transaction System |
| **Language** | Java 17 (Pure Java — No External Frameworks) |
| **Domain** | Operating Systems / Concurrent Programming |
| **Architecture** | Console App + Web-Based REST API with Browser UI |
| **Web Server** | `com.sun.net.httpserver.HttpServer` (Built-in Java) |
| **Date** | March 2026 |

---

## TEAM MEMBERS

| S.No | Name | Roll Number |
|:----:|------|:-----------:|
| 1 | **BAALA GANESH R** | 24EC011 |
| 2 | **K PRIYA DHARSHAN** | 25LEC10 |
| 3 | **M AJAY KUMAR** | 25LEC02 |
| 4 | **SRI NIWAS** | 25LEC13 |

---

## TABLE OF CONTENTS

| S.No | Chapter | Page |
|:----:|---------|:----:|
| 1 | Abstract | 1 |
| 2 | Introduction | 1 |
| 3 | Concurrency Concepts Implemented | 2 |
| 4 | System Architecture | 3 |
| 5 | Module Descriptions | 4 |
| 6 | Key Design Patterns | 6 |
| 7 | Output Screenshots | 7 |
| 8 | How to Run | 8 |
| 9 | Technology Stack | 9 |
| 10 | Features Summary | 9 |
| 11 | Conclusion | 10 |
| 12 | Team Contributions | 10 |
| 13 | References | 10 |

---

## 1. ABSTRACT

This project implements a **Multi-Threaded Bank Transaction System** in pure Java that demonstrates 12+ core Java concurrency concepts through a real-world banking application. The system manages concurrent deposits, withdrawals, fund transfers, and interest calculations across multiple bank accounts, ensuring thread safety and data integrity.

The project features a **browser-based real-time dashboard** with live thread monitoring, stress testing capabilities, race condition demonstrations, and transaction analytics — all powered by Java's built-in HTTP server. It serves as a comprehensive, hands-on demonstration of concurrent programming principles including thread synchronization, lock ordering for deadlock prevention, producer-consumer patterns, atomic operations, and scheduled task management.

---

## 2. INTRODUCTION

### 2.1 Background

Concurrent programming is fundamental to modern software development. Banking systems are among the most critical applications requiring thread safety — a single race condition can lead to lost money, duplicate transactions, or system deadlocks. This project uses the banking domain as a practical context to explore and demonstrate Java's concurrency utilities.

### 2.2 Problem Statement

Design and implement a banking system that:
- Handles multiple simultaneous transactions without data corruption
- Prevents deadlocks during fund transfers between accounts
- Demonstrates the difference between thread-safe and unsafe operations
- Provides real-time visibility into thread states and system performance
- Operates entirely in Java without external dependencies

### 2.3 Objectives

1. Implement thread-safe banking operations (deposit, withdraw, transfer)
2. Demonstrate 12+ Java concurrency concepts in a single application
3. Prevent deadlocks using lock ordering strategy
4. Build a browser-based UI for interactive concurrency demonstrations
5. Provide real-time JVM thread monitoring via JMX
6. Include stress testing and race condition visualization tools

---

## 3. CONCURRENCY CONCEPTS IMPLEMENTED

The following 12 Java concurrency concepts are demonstrated throughout the project:

| # | Concept | Where Used | Purpose |
|---|---------|-----------|---------|
| 1 | **Thread & Runnable** | `TransactionWorker.java` | Worker threads simulate concurrent bank tellers |
| 2 | **synchronized** | `BankService.java` | Mutual exclusion for critical banking operations |
| 3 | **volatile** | `TransactionLogger.java` | Thread-safe stop signal for logger thread |
| 4 | **ReentrantReadWriteLock** | `Account.java` | Multiple readers OR single writer for account balance |
| 5 | **ReentrantLock** | `TransferService.java` | Explicit locking with lock ordering for transfers |
| 6 | **AtomicLong / AtomicInteger** | `TransactionIdGenerator.java`, `Account.java` | Lock-free counters using CAS operations |
| 7 | **BlockingQueue** | `TransactionLogger.java` | Producer-Consumer pattern for transaction logging |
| 8 | **ExecutorService** | `TransferService.java`, `BankWebServer.java` | Thread pool management for workers and HTTP requests |
| 9 | **ScheduledExecutorService** | `InterestService.java`, `ScheduledTasks.java` | Periodic interest calculation and scheduled tasks |
| 10 | **Future.get(timeout)** | `TransferService.java` | Timeout handling for potentially stuck transfers |
| 11 | **CountDownLatch** | `InterestService.java`, `Main.java` | Wait for batch of threads to complete |
| 12 | **ConcurrentHashMap** | `BankService.java` | Thread-safe account registry without locking |
| 13 | **CyclicBarrier** | `ScheduledTasks.java` | Synchronization point for coordinated batch operations |
| 14 | **Deadlock Detection (JMX)** | `DeadlockDetector.java` | Runtime deadlock monitoring via `ThreadMXBean` |

---

## 4. SYSTEM ARCHITECTURE

### 4.1 Package Structure

```
BankSystem/
├── src/com/banksystem/
│   ├── model/                    # Data models
│   │   ├── Account.java          # Thread-safe bank account (ReadWriteLock)
│   │   ├── Customer.java         # Customer with AtomicLong ID
│   │   └── Transaction.java      # Immutable transaction record
│   ├── service/                  # Business logic
│   │   ├── BankService.java      # Core banking (ConcurrentHashMap)
│   │   ├── TransferService.java  # Deadlock-safe transfers (Lock Ordering)
│   │   └── InterestService.java  # Scheduled interest (ScheduledExecutorService)
│   ├── threading/                # Concurrency components
│   │   ├── TransactionLogger.java    # Producer-Consumer (BlockingQueue)
│   │   ├── TransactionWorker.java    # Runnable worker threads
│   │   └── ScheduledTasks.java       # CyclicBarrier + scheduled tasks
│   ├── util/                     # Utilities
│   │   ├── DeadlockDetector.java         # JMX deadlock monitoring
│   │   └── TransactionIdGenerator.java   # AtomicLong lock-free IDs
│   ├── web/
│   │   └── BankWebServer.java    # REST API + HTTP server
│   ├── Main.java                 # Console-based entry point
│   └── WebMain.java              # Web-based entry point
├── web/
│   └── index.html                # Browser UI (5-tab dashboard)
├── Dockerfile                    # Container deployment
└── deploy.sh                     # Oracle Cloud deploy script
```

### 4.2 Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    BROWSER (index.html)                       │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌──────┐ ┌───────┐│
│  │ Banking  │ │ Thread    │ │ Stress   │ │ Race │ │Analyt-││
│  │   Tab    │ │ Monitor   │ │  Test    │ │Cond. │ │ ics   ││
│  └────┬─────┘ └─────┬─────┘ └────┬─────┘ └──┬───┘ └───┬───┘│
└───────┼─────────────┼────────────┼───────────┼─────────┼────┘
        │  REST API   │   (JSON)   │           │         │
═══════════════════════════════════════════════════════════════
        │             │            │           │         │
┌───────▼─────────────▼────────────▼───────────▼─────────▼────┐
│               BankWebServer.java (Port 8080)                 │
│         HttpServer + ExecutorService (Thread Pool)           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ /api/accounts  /api/deposit  /api/withdraw           │   │
│  │ /api/transfer  /api/transactions  /api/threads       │   │
│  │ /api/stresstest  /api/racedemo  /api/analytics       │   │
│  │ /api/createaccount                                   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────┬──────────┬──────────────┬─────────────────────────┘
          │          │              │
  ┌───────▼───┐ ┌────▼─────┐ ┌─────▼──────┐
  │  Bank     │ │ Transfer │ │ Interest   │
  │ Service   │ │ Service  │ │ Service    │
  │(Concurrent│ │(Lock     │ │(Scheduled  │
  │ HashMap)  │ │ Ordering)│ │ Executor)  │
  └─────┬─────┘ └────┬─────┘ └─────┬──────┘
        │            │              │
        └──────┬─────┘       ┌──────┘
               │             │
  ┌────────────▼─────────────▼──────────────┐
  │           Account.java                   │
  │     (ReentrantReadWriteLock)             │
  │  ReadLock: getBalance() [concurrent]     │
  │  WriteLock: deposit/withdraw [exclusive] │
  └────────────┬────────────────────────────┘
               │
  ┌────────────▼────────────────────────────┐
  │      TransactionLogger (BlockingQueue)   │
  │  Producers → Queue(1000) → Consumer      │
  │     (volatile stop flag + daemon)        │
  └──────────────────────────────────────────┘
               │
  ┌────────────▼────────────────────────────┐
  │      DeadlockDetector (JMX)              │
  │  ThreadMXBean.findDeadlockedThreads()    │
  │     Background daemon thread             │
  └──────────────────────────────────────────┘
```

---

## 5. MODULE DESCRIPTIONS

### 5.1 Model Layer

**Account.java (~85 lines)**
Thread-safe bank account model. Uses `ReentrantReadWriteLock` to allow multiple threads to read balances simultaneously while ensuring exclusive access during deposits and withdrawals. Account IDs are generated atomically using `AtomicLong`.

**Transaction.java (~60 lines)**
Immutable transaction record with enums for transaction type (DEPOSIT, WITHDRAWAL, TRANSFER, INTEREST) and status (SUCCESS, FAILED, TIMEOUT, PENDING). Being immutable, it is inherently thread-safe.

**Customer.java (~30 lines)**
Immutable customer data model. Uses `AtomicLong` for thread-safe ID generation.

### 5.2 Service Layer

**BankService.java (~120 lines)**
Core banking operations manager. Maintains account registry using `ConcurrentHashMap` — enabling lock-free thread-safe access to accounts. Handles deposits, withdrawals, and account creation. Acts as a producer in the producer-consumer pattern, sending completed transactions to the `TransactionLogger`.

**TransferService.java (~150 lines)**
Handles fund transfers with **deadlock prevention via lock ordering**. When transferring between Account A and Account B, it always locks the account with the smaller ID first. This consistent ordering eliminates circular wait — one of the four conditions for deadlock. Uses `ExecutorService` for thread pool management and `Future.get(timeout)` to detect stuck transfers.

**InterestService.java (~100 lines)**
Calculates and applies interest to all accounts at scheduled intervals using `ScheduledExecutorService`. Uses `CountDownLatch` to wait for all parallel interest calculations across accounts before reporting completion.

### 5.3 Threading Layer

**TransactionLogger.java (~110 lines)**
Implements the **Producer-Consumer pattern** using `LinkedBlockingQueue`. Banking services produce transactions into the queue; the logger thread consumes them. Uses `volatile` for the `running` flag to ensure thread-safe shutdown signaling. The queue capacity of 1000 provides back-pressure when transactions arrive faster than they can be logged.

**TransactionWorker.java (~90 lines)**
A `Runnable` that simulates concurrent bank activity. Each worker performs a configurable number of random operations (deposits, withdrawals, transfers) on random accounts. Used by the stress test feature to load the system with concurrent transactions.

**ScheduledTasks.java (~90 lines)**
Manages periodic tasks using `ScheduledExecutorService`. Provides `CyclicBarrier` creation for coordinating batch operations where all threads must reach a synchronization point before proceeding.

### 5.4 Utility Layer

**DeadlockDetector.java (~120 lines)**
Background daemon thread that monitors for deadlocks using JVM's `ThreadMXBean` from the Java Management Extensions (JMX) API. `findDeadlockedThreads()` detects cycles in the lock dependency graph and reports detailed thread info including lock owners and stack traces.

**TransactionIdGenerator.java (~40 lines)**
Lock-free transaction ID generator using `AtomicLong` with Compare-And-Swap (CAS) operations. More performant than `synchronized` for simple counter increments.

### 5.5 Web Layer

**BankWebServer.java (~500+ lines)**
REST API server built on Java's built-in `com.sun.net.httpserver.HttpServer`. Handles concurrent HTTP requests via an `ExecutorService` thread pool. Exposes 10 API endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/` | GET | Serves the HTML dashboard |
| `/api/accounts` | GET | List all accounts with balances |
| `/api/deposit` | POST | Deposit money into an account |
| `/api/withdraw` | POST | Withdraw money from an account |
| `/api/transfer` | POST | Transfer between accounts |
| `/api/transactions` | GET | Get transaction history |
| `/api/threads` | GET | Real-time JVM thread dump via JMX |
| `/api/stresstest` | POST | Launch N concurrent worker threads |
| `/api/racedemo` | POST | AtomicInteger vs plain int demo |
| `/api/analytics` | GET | Transaction breakdown & performance stats |
| `/api/createaccount` | POST | Create new bank account |

**index.html (~500 lines)**
Single-page browser UI with 5 tabbed sections, dark cyberpunk theme, auto-refreshing data (accounts every 2 seconds, threads every 1 second), and mobile-responsive design.

---

## 6. KEY DESIGN PATTERNS

### 6.1 Deadlock Prevention via Lock Ordering

The most critical concurrency pattern in this project. When transferring funds between two accounts, both accounts must be locked simultaneously. Without a strategy, Thread 1 locking A→B while Thread 2 locks B→A creates a **circular wait** (deadlock).

**Solution:** Always lock accounts in ascending ID order:

```java
// In TransferService.java
Account first  = (fromId < toId) ? fromAccount : toAccount;
Account second = (fromId < toId) ? toAccount : fromAccount;

first.getLock().writeLock().lock();
try {
    second.getLock().writeLock().lock();
    try {
        // Perform transfer safely
    } finally {
        second.getLock().writeLock().unlock();
    }
} finally {
    first.getLock().writeLock().unlock();
}
```

This eliminates circular wait regardless of how many threads transfer simultaneously.

### 6.2 Producer-Consumer Pattern

Transaction logging uses a decoupled producer-consumer architecture:

```
BankService  ─┐
TransferService ─┼──▶ BlockingQueue(1000) ──▶ TransactionLogger Thread
InterestService ─┘          ▲                          │
                        (bounded)              Stores in history
```

- **Producers** (services) never block on logging — they enqueue transactions and continue
- **Consumer** (logger) processes transactions asynchronously from the queue
- Queue capacity provides **back-pressure** protection against unbounded growth

### 6.3 ReadWriteLock for Account Access

```
getBalance()     → ReadLock    (multiple threads can read simultaneously)
deposit()        → WriteLock   (exclusive access, blocks all readers/writers)
withdraw()       → WriteLock   (exclusive access)
applyInterest()  → WriteLock   (exclusive access)
```

This is more performant than `synchronized` because balance queries (the most common operation) are fully concurrent.

---

## 7. OUTPUT SCREENSHOTS

### 7.1 Banking Tab — Live Account Dashboard

![Banking Tab](../output/1.png)

**Figure 1:** The Banking tab showing 6 accounts with live balances, 50 transactions processed with 100% success rate, and a total balance of $50,509.15. The account table displays real-time data including customer names, emails, and balances. Below the table are the Deposit, Withdraw, and New Account forms. The transfer form uses deadlock-safe lock ordering (indicated by the badge).

---

### 7.2 Thread Monitor Tab — Real-Time JVM Thread Visualization

![Thread Monitor](../output/2.png)

**Figure 2:** The Thread Monitor tab displaying 22 active JVM threads — 7 Runnable (green), 15 Waiting/Timed Waiting (yellow/blue), 0 Blocked (red), with a peak of 26 threads. The thread state distribution bar visualizes the ratio graphically. The live thread list shows individual threads including `main`, `TxnLogger`, `DeadlockDetector`, `InterestScheduler`, `HTTP-Dispatcher`, and multiple pool threads. The deadlock status indicator at the bottom confirms no deadlocks detected.

---

### 7.3 Analytics Tab — Transaction Breakdown & Performance

![Analytics](../output/3.png)

**Figure 3:** The Analytics tab showing transaction breakdown: 20 Deposits (8%), 15 Withdrawals (6%), 15 Transfers (6%), and 189 Interest (79%). The Money Flow card shows +$4,770.86 deposited, -$3,250.85 withdrawn, and $4,219.21 transferred. The Success/Failure ratio shows 239 successful transactions with 0 failures and 0 timeouts. System Performance: 36 peak threads, 0 active workers, 25 ops/second throughput, and 239 total transactions.

---

## 8. HOW TO RUN

### 8.1 Prerequisites
- Java 17 or higher (`java -version`)
- No external libraries required

### 8.2 Compile
```bash
cd BankSystem
mkdir -p out
javac -d out \
    src/com/banksystem/util/*.java \
    src/com/banksystem/model/*.java \
    src/com/banksystem/threading/*.java \
    src/com/banksystem/service/*.java \
    src/com/banksystem/web/*.java \
    src/com/banksystem/WebMain.java
```

### 8.3 Run Web Server
```bash
java -cp out com.banksystem.WebMain
```
Then open **http://localhost:8080** in a browser.

### 8.4 Run Console Simulation
```bash
javac -d out src/com/banksystem/**/*.java src/com/banksystem/Main.java
java -cp out com.banksystem.Main
```

### 8.5 Docker Deployment
```bash
docker build -t banksystem .
docker run -p 8080:8080 banksystem
```

---

## 9. TECHNOLOGY STACK

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Web Server | `com.sun.net.httpserver.HttpServer` (built-in) |
| Thread Monitoring | Java Management Extensions (JMX) — `ThreadMXBean` |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Data Storage | In-memory (`ConcurrentHashMap`) |
| External Dependencies | **None** — Pure Java |
| Containerization | Docker (multi-stage build) |
| Deployment | Oracle Cloud, systemd service |

---

## 10. FEATURES SUMMARY

| Feature | Description |
|---------|-------------|
| **Deposit / Withdraw** | Thread-safe operations with ReadWriteLock |
| **Fund Transfer** | Deadlock-free via lock ordering strategy |
| **Account Creation** | Dynamic account creation from browser |
| **Transaction Log** | Real-time log powered by BlockingQueue consumer |
| **Thread Monitor** | Live JVM thread states via JMX (1-second refresh) |
| **Deadlock Detection** | Background JMX-based deadlock cycle detection |
| **Stress Test** | Launch concurrent workers from browser |
| **Race Condition Demo** | AtomicInteger vs plain int side-by-side |
| **Analytics Dashboard** | Transaction breakdown, money flow, throughput |
| **Interest Calculator** | Scheduled periodic interest via ScheduledExecutorService |
| **Mobile Responsive** | Fully responsive UI for mobile/tablet/desktop |
| **Cloud Deployable** | Dockerfile + Oracle Cloud deploy script included |

---

## 11. CONCLUSION

This project successfully demonstrates that complex multi-threading concepts can be made tangible and interactive through a real-world banking application. By combining 12+ Java concurrency primitives with a browser-based dashboard, users can visually observe thread behavior, trigger race conditions, monitor for deadlocks, and stress-test the system in real-time.

The key technical achievements include:
- **Zero data corruption** across thousands of concurrent transactions
- **Deadlock prevention** through consistent lock ordering
- **Real-time JVM introspection** via JMX `ThreadMXBean`
- **No external dependencies** — the entire stack runs on pure Java
- **Cloud-ready deployment** with Docker and Oracle Cloud support

The project serves as both an educational tool for understanding Java concurrency and a practical reference implementation for building thread-safe applications.

---

## 12. TEAM CONTRIBUTIONS

| Member | Roll No. | Contribution |
|--------|:--------:|-------------|
| **Baala Ganesh R** | 24EC011 | System architecture, core banking services, web server, deadlock prevention, deployment pipeline |
| **K Priya Dharshan** | 25LEC10 | Thread monitoring module, JMX integration, race condition demonstration, stress test engine |
| **M Ajay Kumar** | 25LEC02 | Transaction logger (producer-consumer), interest service, scheduled tasks, concurrency testing |
| **Sri Niwas** | 25LEC13 | Frontend UI development, analytics dashboard, mobile responsive design, documentation |

---

## 13. REFERENCES

1. Oracle Java Documentation — `java.util.concurrent` package
2. Brian Goetz, *Java Concurrency in Practice*, Addison-Wesley, 2006
3. Oracle Java Tutorials — Lesson: Concurrency
4. Java Management Extensions (JMX) — `ThreadMXBean` API
5. `com.sun.net.httpserver` — Java HTTP Server API Documentation

---

<div align="center">

**Repository:** [github.com/baalaganeshr/Multi-Thread-bank-system-](https://github.com/baalaganeshr/Multi-Thread-bank-system-)

---

*Prepared by: Baala Ganesh R, K Priya Dharshan, M Ajay Kumar, Sri Niwas*

*March 2026*

</div>
