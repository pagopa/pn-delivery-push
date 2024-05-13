/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.middleware.queue.consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTIONEXCEPTION;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_EVENTTYPENOTSUPPORTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionEventType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class PnEventInboundService {
    private final EventHandler eventHandler;
    private final String externalChannelEventQueueName;
    private final String safeStorageEventQueueName;
    private final String nationalRegistriesEventQueueName;
    private final String addressManagerEventQueueName;
    private final String validateF24EventQueueName;
    
    public PnEventInboundService(EventHandler eventHandler, PnDeliveryPushConfigs cfg) {
        this.eventHandler = eventHandler;
        this.externalChannelEventQueueName = cfg.getTopics().getFromExternalChannel();
        this.safeStorageEventQueueName = cfg.getTopics().getSafeStorageEvents();
        this.nationalRegistriesEventQueueName = cfg.getTopics().getNationalRegistriesEvents();
        this.addressManagerEventQueueName = cfg.getTopics().getAddressManagerEvents();
        this.validateF24EventQueueName = cfg.getTopics().getF24Events();
    }

    @Bean
    public MessageRoutingCallback customRouter() {
        return new MessageRoutingCallback() {
            @Override
            public FunctionRoutingResult routingResult(Message<?> message) {
                setMdc(message);
                return new FunctionRoutingResult(handleMessage(message));
            }
        };
    }

    private void setMdc(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        MDCUtils.clearMDCKeys();
        
        if (messageHeaders.containsKey("aws_messageId")){
            String awsMessageId = messageHeaders.get("aws_messageId", String.class);
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }
        
        if (messageHeaders.containsKey("X-Amzn-Trace-Id")){
            String traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }

        String iun = (String) message.getHeaders().get("iun");
        if(iun != null){
            MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
        }
    }

    private String handleMessage(Message<?> message) {
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("Received message from customRouter with eventType={}", eventType);

        String iun = (String) message.getHeaders().get("iun");

        if (eventType != null) {
            //Se l'event type e valorizzato ...
            if (ActionEventType.ACTION_GENERIC.name().equals(eventType)) {
                //... e si tratta di una ACTION, viene gestito con l'handleActionGeneric
                return handleGenericAction(message);
            } else if (WebhookActionEventType.WEBHOOK_ACTION_GENERIC.name().equals(eventType)) {
                //... e si tratta di una WEBHOOK ACTION, viene gestito con l'handleWebhookAction
                return handleWebhookAction();
            }
            else if(eventType.equals("EXTERNAL_CHANNELS_EVENT")) {
                //usato ora dal mock di external-channels, in futuro se viene modificato l'evento, adeguare anche il mock
                eventType = handleOtherEvent(message);
            }
        }else {
            //EXTERNAL CHANNEL dovrà INVIARE UN EventType specifico PN-1998
            
            //Se l'eventType non è valorizzato entro sicuramente qui
            eventType = handleOtherEvent(message);
        }

        /*... arrivati qui, l'eventType o già valorizzato MA non era: ACTION_GENERIC, WEBHOOK_ACTION_GENERIC, EXTERNAL_CHANNELS_EVENT
            dunque rientrano i casi di NEW_NOTIFICATION, NOTIFICATION_VIEWED, NOTIFICATION_PAID ecc. 
            oppure l'eventType non era valorizzato ed è stato valorizzato in handleExternalChannelEvent.
         */

        String handlerName = eventHandler.getHandler().get(eventType);
        if (!StringUtils.hasText(handlerName)) {
            log.error("undefined handler for eventType={}", eventType);
        }

        log.debug("Handler for eventType={} is {} - iun={}", eventType, handlerName, iun);

        return handlerName;
    }

    @NotNull
    private String handleOtherEvent(Message<?> message) {
        String eventType;
        String queueName = (String) message.getHeaders().get("aws_receivedQueue");
        if (Objects.equals(queueName, externalChannelEventQueueName)) {
            eventType = "SEND_PEC_RESPONSE";
        } else if (Objects.equals(queueName, safeStorageEventQueueName)) {
            eventType = "SAFE_STORAGE_EVENTS";
        }
        else if(Objects.equals(queueName, nationalRegistriesEventQueueName)) {
            eventType = "NR_GATEWAY_RESPONSE";
        }
        else if(Objects.equals(queueName, addressManagerEventQueueName)) {
            eventType = "ADDRESS_MANAGER_EVENTS";
        }
        else if(Objects.equals(queueName, validateF24EventQueueName)) {
            eventType = "F24_EVENTS";
        }
        else {
            log.error("eventType not present, cannot start scheduled action headers={} payload={}", message.getHeaders(), message.getPayload());
            throw new PnInternalException("eventType not present, cannot start scheduled action", ERROR_CODE_DELIVERYPUSH_EVENTTYPENOTSUPPORTED);
        }
        return eventType;
    }

    @NotNull
    private String handleWebhookAction() {
        return "pnDeliveryPushWebhookActionConsumer";
    }

    private String handleGenericAction(Message<?> message) {
        /*Quando verrà utilizzata la sola versione v2 verificare se si può evitare di dover gestire la action in modo separato, valorizzando direttamente in fase
            di scheduling l'eventType con il valore del type della action (ActionPoolImpl -> addToActionsQueue)
         */
        Map<String, String> actionMap = getActionMapFromMessage(message);
        String actionType = actionMap.get("type");
        if (actionType != null) {
            String handlerName = eventHandler.getHandler().get(actionType);
            if (!StringUtils.hasText(handlerName)) {
                log.error("undefined handler for actionType={}", actionType);
            }
            return handlerName;
        } else {
            log.error("actionType not present, cannot start scheduled action");
            throw new PnInternalException("actionType not present, cannot start scheduled action", ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED);
        }
    }

    private Map<String, String> getActionMapFromMessage(Message<?> message) {
        try {
            String payload = (String) message.getPayload();
            Map<String, String> action = new ObjectMapper().readValue(payload, HashMap.class);

            if (action == null) {
                log.error("Action is not valid, cannot start scheduled action");
                throw new PnInternalException("Action is not valid, cannot start scheduled action", ERROR_CODE_DELIVERYPUSH_ACTIONEXCEPTION);
            }
            return action;
        } catch (JsonProcessingException ex) {
            log.error("Exception during json mapping ex", ex);
            throw new PnInternalException("Exception during json mapping ex=" + ex, ERROR_CODE_DELIVERYPUSH_ACTIONEXCEPTION);
        }
    }

}
