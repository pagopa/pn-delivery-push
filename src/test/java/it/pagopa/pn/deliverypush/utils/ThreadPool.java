package it.pagopa.pn.deliverypush.utils;

import java.util.ArrayList;
import java.util.List;

public class ThreadPool {
    private static List<Thread> threadPool = null;
    
    private ThreadPool() {}

    public static void start(Thread thread) {
        if ( threadPool == null) {
            threadPool = new ArrayList<>();
        }
        threadPool.add(thread);
        thread.start();
    }
    
    public static void killThreads(){
        if ( threadPool != null) {
            threadPool.forEach( thread ->  thread.interrupt());
            threadPool = null;
        }
    }
}