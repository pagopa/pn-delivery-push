package it.pagopa.pn.deliverypush.service.impl;


import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.service.mapper.ActionManagerMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
@ConditionalOnProperty(name = ActionService.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = "REST")
public class ActionServiceRestImpl implements ActionService
 {
    private final ActionManagerClient actionManagerClient;
    private final ActionManagerMapper actionManagerMapper;

    @Override
    public void addOnlyActionIfAbsent(Action action) {
        log.info("Starting to add action with ID: {}", action.getActionId());
         actionManagerClient.addOnlyActionIfAbsent(actionManagerMapper.fromActionInternalToActionDto(action));
    }

     @Override
     public void unSchedule(String actionId) {
         log.info("Starting to unschedule action with ID: {}", actionId);
         actionManagerClient.unscheduleAction(actionId);
     }

}

