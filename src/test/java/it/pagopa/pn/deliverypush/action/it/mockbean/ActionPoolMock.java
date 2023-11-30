package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ActionPoolMock {
    private final ActionHandlerMock actionHandlerMock;
    
    private List<Action> futureAction;
    
    public ActionPoolMock(ActionHandlerMock actionHandlerMock){
        futureAction = new ArrayList<>();
        this.actionHandlerMock = actionHandlerMock;
    }
    public void clear() {
        this.futureAction = new ArrayList<>();
    }
    
    @Scheduled(fixedRate = 500)
    protected void pollForFutureActions() {
        log.info("[TEST] started new scheduled task");
        try {
            handleExpiredAction();
        }catch (Exception ex){
            log.error("[TEST] Exception in pollForFutureActions", ex);
        }
    }
    
    public void addAction(Action action){
        int index = Collections.binarySearch(futureAction, action, Comparator.comparing(Action::getNotBefore));

        if (index < 0) {
            // Se l'indice è negativo, lo convertiamo in un indice valido per l'inserimento.
            index = -(index + 1);
        }

        futureAction.add(index, action);
    }
    
    public void handleExpiredAction(){
        
        boolean expired = true;
        int i = 0;
        while (expired && i < futureAction.size()) {
            Action action = futureAction.get(i);
            
            if(action.getNotBefore().isBefore(Instant.now())){
                actionHandlerMock.handleSchedulingAction(action);
                futureAction.remove(action);
            } else {
                expired = false;
            }
            i++;
        }
    }

}
