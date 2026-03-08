package com.banksystem.web;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.model.Transaction;
import com.banksystem.service.BankService;
import com.banksystem.service.TransferService;
import com.banksystem.threading.TransactionLogger;
import com.banksystem.threading.TransactionWorker;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Built-in Java HTTP Server with innovative features:
 * - Stress Test: launch N concurrent workers from the browser
 * - Live Thread Monitor: see all JVM threads in real-time
 * - Account Creation: create new accounts from UI
 * - Race Condition Demo: safe vs unsafe concurrent counter
 * - Transaction Analytics: real-time stats
 */
public class BankWebServer {

    private final HttpServer server;
    private final BankService bankService;
    private final TransferService transferService;
    private final TransactionLogger transactionLogger;
    private final AtomicInteger stressTestRunning = new AtomicInteger(0);
    private final AtomicLong totalProcessingTimeNs = new AtomicLong(0);
    private final AtomicLong totalOpsCount = new AtomicLong(0);

    public BankWebServer(BankService bankService, TransferService transferService,
                         TransactionLogger transactionLogger, int port) throws IOException {
        this.bankService = bankService;
        this.transferService = transferService;
        this.transactionLogger = transactionLogger;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // Original routes
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/accounts", new AccountsHandler());
        server.createContext("/api/deposit", new DepositHandler());
        server.createContext("/api/withdraw", new WithdrawHandler());
        server.createContext("/api/transfer", new TransferHandler());
        server.createContext("/api/transactions", new TransactionsHandler());

        // INNOVATIVE: New routes
        server.createContext("/api/threads", new ThreadMonitorHandler());
        server.createContext("/api/stresstest", new StressTestHandler());
        server.createContext("/api/createaccount", new CreateAccountHandler());
        server.createContext("/api/racedemo", new RaceConditionDemoHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
    }

    public void start() {
        server.start();
        System.out.printf("[WebServer] Started on http://localhost:%d%n",
                server.getAddress().getPort());
    }

    public void stop() {
        server.stop(2);
        System.out.println("[WebServer] Stopped.");
    }

    // ──────────── UTILITY METHODS ────────────

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (!body.isEmpty()) {
                for (String pair : body.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(
                            java.net.URLDecoder.decode(kv[0], "UTF-8"),
                            java.net.URLDecoder.decode(kv[1], "UTF-8")
                        );
                    }
                }
            }
        }
        return params;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ──────────── HANDLERS ────────────

    /** Serve the HTML frontend */
    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String basePath = System.getProperty("user.dir");
            Path htmlPath = Paths.get(basePath, "web", "index.html");

            if (Files.exists(htmlPath)) {
                byte[] content = Files.readAllBytes(htmlPath);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                String msg = "index.html not found at: " + htmlPath;
                exchange.sendResponseHeaders(404, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes());
                }
            }
        }
    }

    /** GET /api/accounts — return all accounts as JSON */
    class AccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Account acc : bankService.getAllAccounts()) {
                if (!first) json.append(",");
                json.append(String.format(
                        "{\"id\":%d,\"customer\":\"%s\",\"email\":\"%s\",\"balance\":%.2f}",
                        acc.getAccountId(),
                        escapeJson(acc.getCustomer().getName()),
                        escapeJson(acc.getCustomer().getEmail()),
                        acc.getBalance()));
                first = false;
            }
            json.append("]");
            sendJson(exchange, 200, json.toString());
        }
    }

    /** POST /api/deposit — {accountId, amount} */
    class DepositHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> params = parseFormBody(exchange);
            try {
                long accountId = Long.parseLong(params.get("accountId"));
                double amount = Double.parseDouble(params.get("amount"));

                if (amount <= 0) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Amount must be positive\"}");
                    return;
                }

                boolean success = bankService.deposit(accountId, amount);
                Account acc = bankService.getAccount(accountId);
                String balance = acc != null ? String.format("%.2f", acc.getBalance()) : "0.00";

                sendJson(exchange, 200, String.format(
                        "{\"success\":%s,\"message\":\"%s\",\"newBalance\":%s,\"thread\":\"%s\"}",
                        success,
                        success ? "Deposit successful" : "Deposit failed",
                        balance,
                        escapeJson(Thread.currentThread().getName())));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid input\"}");
            }
        }
    }

    /** POST /api/withdraw — {accountId, amount} */
    class WithdrawHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> params = parseFormBody(exchange);
            try {
                long accountId = Long.parseLong(params.get("accountId"));
                double amount = Double.parseDouble(params.get("amount"));

                if (amount <= 0) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Amount must be positive\"}");
                    return;
                }

                boolean success = bankService.withdraw(accountId, amount);
                Account acc = bankService.getAccount(accountId);
                String balance = acc != null ? String.format("%.2f", acc.getBalance()) : "0.00";

                sendJson(exchange, 200, String.format(
                        "{\"success\":%s,\"message\":\"%s\",\"newBalance\":%s,\"thread\":\"%s\"}",
                        success,
                        success ? "Withdrawal successful" : "Insufficient funds",
                        balance,
                        escapeJson(Thread.currentThread().getName())));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid input\"}");
            }
        }
    }

    /** POST /api/transfer — {fromAccountId, toAccountId, amount} */
    class TransferHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> params = parseFormBody(exchange);
            try {
                long fromId = Long.parseLong(params.get("fromAccountId"));
                long toId = Long.parseLong(params.get("toAccountId"));
                double amount = Double.parseDouble(params.get("amount"));

                if (amount <= 0) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Amount must be positive\"}");
                    return;
                }
                if (fromId == toId) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Cannot transfer to same account\"}");
                    return;
                }

                // Uses deadlock-safe transfer with timeout
                boolean success = transferService.transferWithTimeout(fromId, toId, amount, 5000);

                Account from = bankService.getAccount(fromId);
                Account to = bankService.getAccount(toId);

                sendJson(exchange, 200, String.format(
                        "{\"success\":%s,\"message\":\"%s\",\"fromBalance\":%.2f,\"toBalance\":%.2f,\"thread\":\"%s\"}",
                        success,
                        success ? "Transfer successful" : "Transfer failed (insufficient funds or timeout)",
                        from != null ? from.getBalance() : 0,
                        to != null ? to.getBalance() : 0,
                        escapeJson(Thread.currentThread().getName())));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid input\"}");
            }
        }
    }

    /** GET /api/transactions — return recent transaction history */
    class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            List<Transaction> history = transactionLogger.getTransactionHistory();
            // Return last 50 transactions (most recent first)
            int start = Math.max(0, history.size() - 50);

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (int i = history.size() - 1; i >= start; i--) {
                Transaction txn = history.get(i);
                if (!first) json.append(",");
                json.append(String.format(
                        "{\"id\":%d,\"type\":\"%s\",\"from\":%d,\"to\":%d,\"amount\":%.2f,\"status\":\"%s\",\"description\":\"%s\",\"time\":\"%s\"}",
                        txn.getTransactionId(),
                        txn.getType(),
                        txn.getFromAccountId(),
                        txn.getToAccountId(),
                        txn.getAmount(),
                        txn.getStatus(),
                        escapeJson(txn.getDescription()),
                        txn.getTimestamp().toString()));
                first = false;
            }
            json.append("]");
            sendJson(exchange, 200, json.toString());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INNOVATION 1: LIVE THREAD MONITOR
    //  Shows all JVM threads, states, and grouped counts
    // ═══════════════════════════════════════════════════════
    class ThreadMonitorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threads = mxBean.dumpAllThreads(false, false);
            long[] deadlocked = mxBean.findDeadlockedThreads();

            Map<String, Integer> stateCounts = new HashMap<>();
            StringBuilder threadArr = new StringBuilder("[");
            boolean first = true;

            for (ThreadInfo info : threads) {
                if (!first) threadArr.append(",");
                String state = info.getThreadState().toString();
                stateCounts.merge(state, 1, Integer::sum);

                long cpuTime = mxBean.getThreadCpuTime(info.getThreadId());
                threadArr.append(String.format(
                    "{\"name\":\"%s\",\"id\":%d,\"state\":\"%s\",\"cpuTimeMs\":%d,\"blockedCount\":%d,\"waitedCount\":%d}",
                    escapeJson(info.getThreadName()),
                    info.getThreadId(),
                    state,
                    cpuTime / 1_000_000,
                    info.getBlockedCount(),
                    info.getWaitedCount()));
                first = false;
            }
            threadArr.append("]");

            StringBuilder statesJson = new StringBuilder("{");
            boolean sf = true;
            for (Map.Entry<String, Integer> e : stateCounts.entrySet()) {
                if (!sf) statesJson.append(",");
                statesJson.append(String.format("\"%s\":%d", e.getKey(), e.getValue()));
                sf = false;
            }
            statesJson.append("}");

            String json = String.format(
                "{\"total\":%d,\"deadlocked\":%s,\"states\":%s,\"threads\":%s,\"peakThreads\":%d}",
                threads.length,
                deadlocked != null && deadlocked.length > 0,
                statesJson,
                threadArr,
                mxBean.getPeakThreadCount());

            sendJson(exchange, 200, json);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INNOVATION 2: STRESS TEST - Launch concurrent workers
    //  Simulates heavy banking load from the browser
    // ═══════════════════════════════════════════════════════
    class StressTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            if (stressTestRunning.get() > 0) {
                sendJson(exchange, 409,
                    String.format("{\"success\":false,\"message\":\"Stress test already running (%d workers active)\"}",
                        stressTestRunning.get()));
                return;
            }

            Map<String, String> params = parseFormBody(exchange);
            int workers = Math.min(Integer.parseInt(params.getOrDefault("workers", "5")), 20);
            int txnPerWorker = Math.min(Integer.parseInt(params.getOrDefault("txnPerWorker", "10")), 50);

            Collection<Account> allAccounts = bankService.getAllAccounts();
            if (allAccounts.size() < 2) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Need at least 2 accounts\"}");
                return;
            }

            long[] accountIds = allAccounts.stream().mapToLong(Account::getAccountId).toArray();
            stressTestRunning.set(workers);
            long startTime = System.nanoTime();

            ExecutorService pool = Executors.newFixedThreadPool(workers);
            CountDownLatch latch = new CountDownLatch(workers);

            for (int i = 1; i <= workers; i++) {
                final int wid = i;
                pool.submit(() -> {
                    try {
                        new TransactionWorker(
                            "StressWorker-" + wid,
                            bankService, transferService,
                            accountIds, txnPerWorker
                        ).run();
                    } finally {
                        stressTestRunning.decrementAndGet();
                        latch.countDown();
                    }
                });
            }

            // Wait in background, track completion
            new Thread(() -> {
                try {
                    latch.await(120, TimeUnit.SECONDS);
                    long elapsed = System.nanoTime() - startTime;
                    totalProcessingTimeNs.addAndGet(elapsed);
                    totalOpsCount.addAndGet((long) workers * txnPerWorker);
                    pool.shutdown();
                    System.out.printf("[StressTest] Complete: %d workers x %d txns in %dms%n",
                        workers, txnPerWorker, elapsed / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "StressTestMonitor").start();

            sendJson(exchange, 200, String.format(
                "{\"success\":true,\"message\":\"Stress test launched: %d workers x %d transactions\",\"totalOps\":%d,\"thread\":\"%s\"}",
                workers, txnPerWorker, workers * txnPerWorker,
                escapeJson(Thread.currentThread().getName())));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INNOVATION 3: CREATE ACCOUNT from browser
    // ═══════════════════════════════════════════════════════
    class CreateAccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> params = parseFormBody(exchange);
            String name = params.getOrDefault("name", "").trim();
            String email = params.getOrDefault("email", "").trim();
            double balance = Double.parseDouble(params.getOrDefault("balance", "0"));

            if (name.isEmpty() || name.length() > 100) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Name is required (max 100 chars)\"}");
                return;
            }
            if (email.isEmpty() || !email.contains("@") || email.length() > 200) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Valid email is required\"}");
                return;
            }
            if (balance < 0 || balance > 1_000_000) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Balance must be 0 - 1,000,000\"}");
                return;
            }

            Account acc = bankService.createAccount(new Customer(name, email), balance);

            sendJson(exchange, 200, String.format(
                "{\"success\":true,\"message\":\"Account %d created for %s\",\"accountId\":%d,\"thread\":\"%s\"}",
                acc.getAccountId(), escapeJson(name), acc.getAccountId(),
                escapeJson(Thread.currentThread().getName())));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INNOVATION 4: RACE CONDITION DEMO
    //  Shows why synchronization matters
    // ═══════════════════════════════════════════════════════
    class RaceConditionDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> params = parseFormBody(exchange);
            int threads = Math.min(Integer.parseInt(params.getOrDefault("threads", "10")), 50);
            int increments = Math.min(Integer.parseInt(params.getOrDefault("increments", "1000")), 10000);
            String mode = params.getOrDefault("mode", "safe");
            int expected = threads * increments;

            if ("safe".equals(mode)) {
                // Thread-safe using AtomicInteger
                AtomicInteger safeCounter = new AtomicInteger(0);
                ExecutorService pool = Executors.newFixedThreadPool(threads);
                CountDownLatch latch = new CountDownLatch(threads);

                long start = System.nanoTime();
                for (int t = 0; t < threads; t++) {
                    pool.submit(() -> {
                        for (int i = 0; i < increments; i++) {
                            safeCounter.incrementAndGet();
                        }
                        latch.countDown();
                    });
                }

                try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long elapsed = System.nanoTime() - start;
                pool.shutdown();

                sendJson(exchange, 200, String.format(
                    "{\"mode\":\"safe\",\"expected\":%d,\"actual\":%d,\"correct\":%s,\"timeMs\":%d,\"mechanism\":\"AtomicInteger (CAS)\",\"threads\":%d,\"increments\":%d}",
                    expected, safeCounter.get(), safeCounter.get() == expected,
                    elapsed / 1_000_000, threads, increments));

            } else {
                // UNSAFE: regular int (will produce wrong result)
                final int[] unsafeCounter = {0};
                ExecutorService pool = Executors.newFixedThreadPool(threads);
                CountDownLatch latch = new CountDownLatch(threads);

                long start = System.nanoTime();
                for (int t = 0; t < threads; t++) {
                    pool.submit(() -> {
                        for (int i = 0; i < increments; i++) {
                            unsafeCounter[0]++;  // NOT thread-safe!
                        }
                        latch.countDown();
                    });
                }

                try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long elapsed = System.nanoTime() - start;
                pool.shutdown();

                int lost = expected - unsafeCounter[0];
                sendJson(exchange, 200, String.format(
                    "{\"mode\":\"unsafe\",\"expected\":%d,\"actual\":%d,\"correct\":%s,\"lost\":%d,\"timeMs\":%d,\"mechanism\":\"Plain int (NO sync)\",\"threads\":%d,\"increments\":%d}",
                    expected, unsafeCounter[0], unsafeCounter[0] == expected,
                    lost, elapsed / 1_000_000, threads, increments));
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INNOVATION 5: REAL-TIME ANALYTICS
    //  Transaction breakdown, throughput, performance stats
    // ═══════════════════════════════════════════════════════
    class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<Transaction> history = transactionLogger.getTransactionHistory();
            int deposits = 0, withdrawals = 0, transfers = 0, interest = 0;
            int success = 0, failed = 0, timeout = 0;
            double totalDeposited = 0, totalWithdrawn = 0, totalTransferred = 0;

            for (Transaction txn : history) {
                switch (txn.getType()) {
                    case DEPOSIT: deposits++; totalDeposited += txn.getAmount(); break;
                    case WITHDRAWAL: withdrawals++; totalWithdrawn += txn.getAmount(); break;
                    case TRANSFER: transfers++; totalTransferred += txn.getAmount(); break;
                    case INTEREST: interest++; break;
                }
                switch (txn.getStatus()) {
                    case SUCCESS: success++; break;
                    case FAILED: failed++; break;
                    case TIMEOUT: timeout++; break;
                    default: break;
                }
            }

            double totalBalance = 0;
            for (Account acc : bankService.getAllAccounts()) {
                totalBalance += acc.getBalance();
            }

            long procTimeMs = totalProcessingTimeNs.get() / 1_000_000;
            long ops = totalOpsCount.get();
            double throughput = procTimeMs > 0 ? (ops * 1000.0 / procTimeMs) : 0;

            sendJson(exchange, 200, String.format(
                "{\"totalTransactions\":%d,\"deposits\":%d,\"withdrawals\":%d,\"transfers\":%d,\"interest\":%d," +
                "\"success\":%d,\"failed\":%d,\"timeout\":%d," +
                "\"totalDeposited\":%.2f,\"totalWithdrawn\":%.2f,\"totalTransferred\":%.2f," +
                "\"totalBalance\":%.2f,\"accounts\":%d," +
                "\"stressTestsRunning\":%d,\"throughput\":%.1f,\"peakThreads\":%d}",
                history.size(), deposits, withdrawals, transfers, interest,
                success, failed, timeout,
                totalDeposited, totalWithdrawn, totalTransferred,
                totalBalance, bankService.getAllAccounts().size(),
                stressTestRunning.get(), throughput,
                ManagementFactory.getThreadMXBean().getPeakThreadCount()));
        }
    }
}
