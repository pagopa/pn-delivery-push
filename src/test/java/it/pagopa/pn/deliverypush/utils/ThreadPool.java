package it.pagopa.pn.deliverypush.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPool {
    private static ExecutorService executor = createNewThreadPool();
    
    private ThreadPool() {}

    public static void start(Thread thread) {
        executor.execute(thread);
    }
    
    public static void killThreads() {
        // Ferma la ThreadPool
        executor.shutdownNow();
        awaitTermination();

        executor = createNewThreadPool();
    }

    private static void awaitTermination() {
        try {
            if(! executor.awaitTermination(3, TimeUnit.MINUTES)){
                log.error("Await termination failed");
            }
        } catch (InterruptedException e) {
            log.error("Await termination failed for exception ", e);
        }
    }

    private static ExecutorService createNewThreadPool() {
        return Executors.newScheduledThreadPool(20);
    }
}