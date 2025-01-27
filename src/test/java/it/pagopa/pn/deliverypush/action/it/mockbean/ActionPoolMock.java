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

    @Scheduled(fixedRate = 5000)
    protected void notExpiredAction() {
        log.info("[TEST] This is not expiredAction {}", futureAction.stream().toList());
    }
    
    @Scheduled(fixedRate = 500) //500 millis
    protected void pollForFutureActions() {
        try {
            handleExpiredAction();
        }catch (Exception ex){
            log.error("[TEST] Exception in pollForFutureActions", ex);
        }
    }
    
    public void addAction(Action action){
        try {
            int index = Collections.binarySearch(futureAction, action, Comparator.comparing(Action::getNotBefore));

            if (index < 0) {
                // Se l'indice è negativo, lo convertiamo in un indice valido per l'inserimento.
                index = -(index + 1);
            }
            futureAction.add(index, action);
            log.info("[TEST] Action addedd to futureAction={}", futureAction);
        }catch (Exception ex){
            log.error("ERROR in add action ex ", ex);
        }
    }
    
    public void handleExpiredAction(){
        boolean expired = true;
        int i = 0;
        
        while (expired && i < futureAction.size()) {
            Action action = futureAction.get(i);
            
            if(action.getNotBefore().isBefore(Instant.now())){
                log.info("[TEST] action to schedule now is={}", action);
                actionHandlerMock.handleSchedulingAction(action);
                futureAction.remove(action);
            } else {
                log.info("[TEST] action is not expired {}", action);
                expired = false;
            }
            i++;
        }
    }

}