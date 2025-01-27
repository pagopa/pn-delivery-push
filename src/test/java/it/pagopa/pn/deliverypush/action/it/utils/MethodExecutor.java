package it.pagopa.pn.deliverypush.action.it.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class MethodExecutor {
    static int millisToWaitEachExecution = 300;
    static int maxMillisToWait = 20000;

    public static void waitForExecution(Supplier<Optional<?>> methodSupplier) { 
        int waitedTime = 0;
        boolean conditionRespected = false;
        while (waitedTime < maxMillisToWait && ! conditionRespected) {
            try {
                Thread.sleep(millisToWaitEachExecution);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            waitedTime = waitedTime + millisToWaitEachExecution;

            Optional<?> result = methodSupplier.get();
            if (result.isEmpty()) {
                log.info("[TEST] Result is not present");
            } else {
                log.info("[TEST] Result is present: {}", result.get());
                conditionRespected = true;
            }
        }
        if(! conditionRespected){
            log.error("[TEST] Time has expired and the condition has not occurred : {}",methodSupplier);
            throw new RuntimeException("[TEST] Time has expired and the condition has not occurred");
        }
    }
}
