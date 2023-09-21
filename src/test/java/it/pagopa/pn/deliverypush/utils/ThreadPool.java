package it.pagopa.pn.deliverypush.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static ExecutorService executor = createNewThreadPool();
    
    private ThreadPool() {}

    public static void start(Thread thread) {
        executor.execute(thread);
    }
    
    public static void killThreads(){
        // Ferma la ThreadPool
        executor.shutdown();
        executor = createNewThreadPool();
    }

    private static ExecutorService createNewThreadPool() {
        return Executors.newFixedThreadPool(10);
    }
}