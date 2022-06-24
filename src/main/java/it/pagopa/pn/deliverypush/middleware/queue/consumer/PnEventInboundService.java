/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.middleware.queue.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEventType;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.impl.WebhookActionEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@Slf4j
public class PnEventInboundService {
    private final EventHandler eventHandler;
    private final PnDeliveryPushConfigs cfg;
    private final String externalChannelEventQueueName;

    public PnEventInboundService(EventHandler eventHandler, PnDeliveryPushConfigs cfg) {
        this.eventHandler = eventHandler;
        this.cfg = cfg;
        this.externalChannelEventQueueName = cfg.getTopics().getFromExternalChannel();
    }

    @Bean
    public MessageRoutingCallback customRouter() {
       return message -> {
           log.debug("messaggio ricevuto da customRouter "+message);
           String eventType = (String) message.getHeaders().get("eventType");
           log.debug("messaggio ricevuto da customRouter eventType={}", eventType );
           if(eventType != null){
               if(ActionEventType.ACTION_GENERIC.name().equals(eventType)) 
                   return handleAction(message);
               else if(WebhookActionEventType.WEBHOOK_ACTION_GENERIC.name().equals(eventType))
                   return "pnDeliveryPushWebhookActionConsumer";
           }else {
               String queueName = (String) message.getHeaders().get("aws_receivedQueue");
               if( Objects.equals( queueName, externalChannelEventQueueName) ) {
                   eventType = "SEND_PAPER_RESPONSE";
               }
               else {
                   log.error("eventType not present, cannot start scheduled action");
                   throw new PnInternalException("eventType not present, cannot start scheduled action");
               }
           }

           String handlerName = eventHandler.getHandler().get(eventType);
           if( ! StringUtils.hasText( handlerName) ) {
               log.error("undefined handler for eventType={}", eventType);
           }
           return handlerName;
       };
    }

    private String handleAction(Message<?> message) {
        /*TODO Quando verrà utilizzata la sola versione v2 verificare se si può evitare di dover gestire la action in modo separato, valorizzando direttamente in fase
            di scheduling l'eventType con il valore del type della action (ActionPoolImpl -> addToActionsQueue)
         */
            Map<String, String> actionMap = getActionMapFromMessage(message);
            String actionType = actionMap.get("type");
            if(actionType != null){
                String handlerName = eventHandler.getHandler().get(actionType);
                if( ! StringUtils.hasText( handlerName) ) {
                    log.error("undefined handler for actionType={}", actionType);
                }
                return handlerName;
            } else {
                log.error("actionType not present, cannot start scheduled action");
                throw new PnInternalException("actionType not present, cannot start scheduled action");
            }
    }

    private Map<String, String> getActionMapFromMessage(Message<?> message) {
        try {
            String payload = (String) message.getPayload();
            Map<String,String> action = new ObjectMapper().readValue(payload, HashMap.class);
            
            if(action == null){
                log.error("Action is not valid, cannot start scheduled action");
                throw new PnInternalException("Action is not valid, cannot start scheduled action");
            }
            return action;
        } catch (JsonProcessingException ex) {
            log.error("Exception during json mapping ex", ex);
            throw new PnInternalException("Exception during json mapping ex="+ ex);
        }
    }
    
}
