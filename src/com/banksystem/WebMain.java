package com.banksystem;

import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.service.BankService;
import com.banksystem.service.InterestService;
import com.banksystem.service.TransferService;
import com.banksystem.threading.TransactionLogger;
import com.banksystem.util.DeadlockDetector;
import com.banksystem.web.BankWebServer;

/**
 * Web-based entry point for the Multi-Threaded Bank System.
 * Starts an HTTP server on port 8080 with REST API + browser UI.
 * 
 * All multi-threading concepts are still active:
 * - HTTP requests handled by thread pool (ExecutorService)
 * - Transfers use deadlock-safe lock ordering
 * - Transaction logging via BlockingQueue (producer-consumer)
 * - Interest calculated by ScheduledExecutorService
 * - Deadlock detection running in background
 */
public class WebMain {

    public static void main(String[] args) throws Exception {

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   MULTI-THREADED BANK SYSTEM — WEB EDITION          ║");
        System.out.println("║   Open http://localhost:8080 in your browser         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // 1. Start Transaction Logger (BlockingQueue Consumer Thread)
        TransactionLogger logger = new TransactionLogger();
        Thread loggerThread = new Thread(logger, "TxnLogger");
        loggerThread.setDaemon(true);
        loggerThread.start();
        System.out.println("[✓] Transaction Logger started (BlockingQueue consumer)");

        // 2. Start Deadlock Detector (JMX Background Monitor)
        DeadlockDetector deadlockDetector = new DeadlockDetector(3000);
        Thread detectorThread = new Thread(deadlockDetector, "DeadlockDetector");
        detectorThread.setDaemon(true);
        detectorThread.start();
        System.out.println("[✓] Deadlock Detector started (JMX ThreadMXBean)");

        // 3. Initialize Services
        BankService bankService = new BankService(logger);
        TransferService transferService = new TransferService(bankService, logger);
        InterestService interestService = new InterestService(bankService, logger, 0.02);
        System.out.println("[✓] Banking services initialized");

        // 4. Create Sample Accounts
        bankService.createAccount(new Customer("Alice Johnson", "alice@bank.com"), 5000.00);
        bankService.createAccount(new Customer("Bob Smith", "bob@bank.com"), 3000.00);
        bankService.createAccount(new Customer("Charlie Brown", "charlie@bank.com"), 7500.00);
        bankService.createAccount(new Customer("Diana Prince", "diana@bank.com"), 2000.00);
        bankService.createAccount(new Customer("Edward Norton", "edward@bank.com"), 10000.00);
        System.out.println("[✓] 5 sample accounts created");

        // 5. Start Scheduled Interest (every 30 seconds for demo)
        interestService.startScheduledInterest(30);
        System.out.println("[✓] Interest service scheduled (2% every 30s)");

        // 6. Start Web Server
        String envPort = System.getenv("PORT");
        int port = (envPort != null) ? Integer.parseInt(envPort) : 8080;
        BankWebServer webServer = new BankWebServer(bankService, transferService, logger, port);
        webServer.start();
        System.out.println("[✓] Web server started on http://localhost:" + port);

        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.println("  Open your browser: http://localhost:" + port);
        System.out.println("  Press Ctrl+C to stop the server");
        System.out.println("══════════════════════════════════════════════════════\n");

        // Keep running until Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Stopping services...");
            webServer.stop();
            transferService.shutdown();
            interestService.shutdown();
            deadlockDetector.stop();
            logger.stop();
            System.out.println("[Shutdown] All services stopped. Goodbye!");
        }));

        // Block main thread
        Thread.currentThread().join();
    }
}
