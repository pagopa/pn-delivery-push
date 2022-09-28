/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.middleware.queue.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionEventType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
           setTraceId(message);
           return handleMessage(message);
       };
    }

    private void setTraceId(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();

        String trace_id = "";

        if (messageHeaders.containsKey("iun"))
            trace_id = messageHeaders.get("iun", String.class);
        else if (messageHeaders.containsKey("aws_messageId"))
            trace_id = messageHeaders.get("aws_messageId", String.class);
        else
         trace_id = "trace_id:" + UUID.randomUUID().toString();

        MDC.put(MDCWebFilter.MDC_TRACE_ID_KEY, trace_id);
    }

    private String handleMessage(Message<?> message) {
        //Viene ricevuto un nuovo evento da una queue
        log.debug("Received message from customRouter {}", message);
        
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("Received message from customRouter with eventType={}", eventType );
        
        if(eventType != null){
            //Se l'event type e valorizzato ...
            if(ActionEventType.ACTION_GENERIC.name().equals(eventType)){
                //... e si tratta di una ACTION, viene gestito con l'handleActionGeneric
                return handleGenericAction(message);
            }
            else if(WebhookActionEventType.WEBHOOK_ACTION_GENERIC.name().equals(eventType)){
                //... e si tratta di una WEBHOOK ACTION, viene gestito con l'handleWebhookAction
                return handleWebhookAction();
            }
            else if(eventType.equals("EXTERNAL_CHANNELS_EVENT")) {
                //TODO usato ora dal mock di external-channels, in futuro se viene modificato l'evento, adeguare anche il mock
                eventType = handleExternalChannelEvent(message);
            }
        }else {
            //TODO EXTERNAL CHANNEL dovrà INVIARE UN EventType specifico
            //Se l'eventType non è valorizzato entro sicuramente qui, cioè negli eventi di externalChannel
            eventType = handleExternalChannelEvent(message);
        }

        /*... arrivati qui, l'eventType o era valorizzato ma non è ne il caso di ACTION o WEBHOOK_ACTION, rientrano i casi di NEW_NOTIFICATION, NOTIFICATION_VIEWED, NOTIFICATION_PAID ecc. 
            oppure l'eventType non era valorizzato ed è stato valorizzato in handleExternalChannelEvent.
         */

        String handlerName = eventHandler.getHandler().get(eventType);
        if( ! StringUtils.hasText( handlerName) ) {
            log.error("undefined handler for eventType={}", eventType);
        }
        return handlerName;
    }

    @NotNull
    private String handleExternalChannelEvent(Message<?> message) {
        String eventType;
        String queueName = (String) message.getHeaders().get("aws_receivedQueue");
        if( Objects.equals( queueName, externalChannelEventQueueName) ) {
            eventType = "SEND_PAPER_RESPONSE";
        }
        else {
            log.error("eventType not present, cannot start scheduled action headers={} payload={}", message.getHeaders(), message.getPayload());
            throw new PnInternalException("eventType not present, cannot start scheduled action");
        }
        return eventType;
    }

    @NotNull
    private String handleWebhookAction() {
        return "pnDeliveryPushWebhookActionConsumer";
    }

    private String handleGenericAction(Message<?> message) {
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
