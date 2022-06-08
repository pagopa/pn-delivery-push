/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.middleware.eventbinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEventType;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

@Configuration
@Slf4j
@ConditionalOnProperty( name = "pn.delivery-push.featureflags.workflow", havingValue = "v2")
public class PnEventInboundService {
    //TODO Trovare un modo per gestire gli errori globalmente
    
    private final StartWorkflowHandler startWorkflowHandler;
    private final ExternalChannelResponseHandler externalChannelResponseHandler;
    private final NotificationViewedHandler notificationViewedHandler;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final EventHandler eventHandler;

    public PnEventInboundService(StartWorkflowHandler startWorkflowHandler, ExternalChannelResponseHandler externalChannelResponseHandler, 
                                 NotificationViewedHandler notificationViewedHandler, DigitalWorkFlowHandler digitalWorkFlowHandler, 
                                 AnalogWorkflowHandler analogWorkflowHandler, RefinementHandler refinementHandler, EventHandler eventHandler) {
        this.startWorkflowHandler = startWorkflowHandler;
        this.externalChannelResponseHandler = externalChannelResponseHandler;
        this.notificationViewedHandler = notificationViewedHandler;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.eventHandler = eventHandler;
    }

    @Bean
    public MessageRoutingCallback customRouter() {
       return message -> {
           log.info("messaggio ricevuto da customRouter "+message);
           String eventType = (String) message.getHeaders().get("eventType");
           log.info("messaggio ricevuto da customRouter eventType={}", eventType );
           if(eventType != null){
               if(ActionEventType.ACTION_GENERIC.name().equals(eventType)) 
                   return handleAction(message);
           }else {
               log.error("eventType not present, cannot start scheduled action");
               throw new PnInternalException("eventType not present, cannot start scheduled action");
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
            log.error("Exception during json mapping ex={}", ex);
            throw new PnInternalException("Exception during json mapping ex="+ ex);
        }
    }

    @Bean
    public Consumer<Message<PnDeliveryNewNotificationEvent.Payload>> pnDeliveryNewNotificationEventConsumer() {
        return message -> {
            try{
                log.info("New notification event received, message {}", message);

                PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                        .payload(message.getPayload())
                        .header(mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();

                startWorkflowHandler.startWorkflow(iun);

            }catch (Exception ex){
                handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<PnDeliveryNotificationViewedEvent.Payload>> pnDeliveryNotificationViewedEventConsumer() {
        return message -> {
            try {
                log.info("Notification viewed event received, message {}", message);

                PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                        .payload(message.getPayload())
                        .header(mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
                int recipientIndex = pnDeliveryNewNotificationEvent.getPayload().getRecipientIndex();
                log.info("pnDeliveryNotificationViewedEventConsumer - iun {}", iun);

                notificationViewedHandler.handleViewNotification(iun, recipientIndex);
            } catch (Exception ex) {
                handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<SingleStatusUpdate>>  pnExtChannelEventInboundConsumer() {
        return message -> {
            try {
                log.info("External channel event received, message {}", message);

                SingleStatusUpdate singleStatusUpdate = message.getPayload();
                externalChannelResponseHandler.extChannelResponseReceiver(singleStatusUpdate);
            } catch (Exception ex) {
                handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushAnalogWorkflowConsumer() {
        return message -> {
            log.info("pnDeliveryPushAnalogWorkflowConsumer, message {}", message);
            Action action = message.getPayload();
            analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushRefinementConsumer() {
        return message -> {
            log.info("pnDeliveryPushRefinementConsumer, message {}", message);
            Action action = message.getPayload();
            refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        return message -> {
            log.info("pnDeliveryPushDigitalNextActionConsumer, message {}", message);
            Action action = message.getPayload();
            digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex());
        };
    }
    
    private StandardEventHeader mapStandardEventHeader(MessageHeaders headers) {
        return StandardEventHeader.builder()
                .eventId((String) headers.get(PN_EVENT_HEADER_EVENT_ID))
                .iun((String) headers.get(PN_EVENT_HEADER_IUN))
                .eventType((String) headers.get(PN_EVENT_HEADER_EVENT_TYPE))
                .createdAt(mapInstant(headers.get(PN_EVENT_HEADER_CREATED_AT)))
                .publisher((String) headers.get(PN_EVENT_HEADER_PUBLISHER))
                .build();
    }

    private Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }
    
    private void handleException(MessageHeaders headers, Exception ex) {
        if(headers != null){
            StandardEventHeader standardEventHeader = mapStandardEventHeader(headers);
            log.error("Generic exception for iun={} ex={}", standardEventHeader.getIun(), ex);
        }else {
            log.error("Generic exception ex={}", ex);
        }
    }
    
}
