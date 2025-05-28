package it.pagopa.pn.deliverypush.service.impl;


import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.service.mapper.ActionManagerMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
@ConditionalOnProperty(name = ActionService.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = "REST", matchIfMissing = true)
public class ActionServiceRestImpl implements ActionService
 {
    private final ActionManagerClient actionManagerClient;
    private final ActionManagerMapper actionManagerMapper;

    @Override
    public void addOnlyActionIfAbsent(Action action) {
        log.info("Starting to add action with ID: {}", action.getActionId());
         actionManagerClient.addOnlyActionIfAbsent(actionManagerMapper.fromActionInternalToActionDto(action));
    }

    //non verrà richiamato
     @Override
     public Optional<Action> getActionById(String actionId) {
         return Optional.empty();
     }

     @Override
     public void unSchedule(Action action, String timeSlot) {
         log.info("Starting to unschedule action with ID: {}", action.getActionId());
         actionManagerClient.unscheduleAction(timeSlot, action.getActionId());
     }

}

