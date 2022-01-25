/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.binding;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action2.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action2.StartWorkflowHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;
import java.util.Collections;
import java.util.function.Consumer;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

@Configuration
@Slf4j
public class PnEventInboundService {
    private final StartWorkflowHandler startWorkflowHandler;
    private final ExternalChannelResponseHandler externalChannelResponseHandler;
    private final NotificationViewedHandler notificationViewedHandler;
    private final EventHandler eventHandler;

    public PnEventInboundService(StartWorkflowHandler startWorkflowHandler, ExternalChannelResponseHandler externalChannelResponseHandler, NotificationViewedHandler notificationViewedHandler, EventHandler eventHandler) {
        this.startWorkflowHandler = startWorkflowHandler;
        this.externalChannelResponseHandler = externalChannelResponseHandler;
        this.notificationViewedHandler = notificationViewedHandler;
        this.eventHandler = eventHandler;
    }
    
    @Bean
    public MessageRoutingCallback customRouter() {
       return message -> {
           System.out.println("messaggio ricevuto da customRouter "+message);
           String eventType = (String) message.getHeaders().get("eventType");
           log.debug("New notification event received, eventType {}",eventType);
           return eventHandler.getHandler().get(eventType);
       };
         
    }

    @Bean
    public Consumer<Message<PnDeliveryNewNotificationEvent.Payload>> pnDeliveryNewNotificationEventConsumer() {
        return (message) -> {
            log.info("pnDeliveryNewNotificationEventConsumer {}", message);

            PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                    .payload(message.getPayload())
                    .header(mapStandardEventHeader(message.getHeaders()))
                    .build();

            String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
            log.info("pnDeliveryNewNotificationEventConsumer - iun {}", iun);

            startWorkflowHandler.startWorkflow(iun);
        };
    }

    @Bean
    public Consumer<Message<PnDeliveryNotificationViewedEvent.Payload>> pnDeliveryNotificationViewedEventConsumer() {
        return (message) -> {
            log.info("pnDeliveryNewNotificationEventConsumer {}", message);

            PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                    .payload(message.getPayload())
                    .header(mapStandardEventHeader(message.getHeaders()))
                    .build();

            String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
            int recipientIndex = pnDeliveryNewNotificationEvent.getPayload().getRecipientIndex();
            log.info("pnDeliveryNotificationViewedEventConsumer - iun {}", iun);

            notificationViewedHandler.handleViewNotification(iun, recipientIndex);
        };
    }

    @Bean
    public Consumer<Message<PnExtChnProgressStatusEventPayload>>  pnExtChannelEventInboundConsumer() {
        return (message) -> {
            System.out.println("pnExtChannelEventInboundConsumer");
            
            PnExtChnProgressStatusEvent evt = PnExtChnProgressStatusEvent.builder()
                    .payload(message.getPayload())
                    .header(mapStandardEventHeader(message.getHeaders()))
                    .build();

            log.info("EXT_CHANNEL RESPONSE iun {} eventId {} correlationId {}", evt.getHeader().getIun(), evt.getHeader().getEventId(), evt.getPayload().getRequestCorrelationId());

            //TODO Questa logica è da cambiare nel momento in cui si applica la logica prevista di OK, RETRYABLE_FAIL, PERMANENT_FAIL 
            ExtChannelResponseStatus status = PnExtChnProgressStatus.OK.equals(evt.getPayload().getStatusCode()) ? ExtChannelResponseStatus.OK : ExtChannelResponseStatus.KO;

            ExtChannelResponse response = ExtChannelResponse.builder()
                    .responseStatus(status)
                    .eventId(evt.getPayload().getRequestCorrelationId())
                    .analogNewAddressFromInvestigation(evt.getPayload().getNewPhysicalAddress())
                    .notificationDate(evt.getPayload().getStatusDate())
                    .iun(evt.getPayload().getIun())
                    .attachmentKeys(evt.getPayload().getAttachmentKeys())
                    .errorList(Collections.singletonList(status.name())) //TODO La logica con cui viene valorizzato l'error list non mi è chiara, cosa dovrebbe esserci all'interno?
                    .build();

            externalChannelResponseHandler.extChannelResponseReceiver(response);
            
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
    
}
