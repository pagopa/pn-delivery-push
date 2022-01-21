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
import it.pagopa.pn.deliverypush.action2.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.temp.mom.consumer.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

@Configuration
@Slf4j
public class PnEventInboundService {
    private final Map<String, EventHandler<?>> handlers;
    private StartWorkflowHandler startWorkflowHandler;
    private ExternalChannelResponseHandler externalChannelResponseHandler;

    public PnEventInboundService(StartWorkflowHandler startWorkflowHandler, ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.startWorkflowHandler = startWorkflowHandler;
        this.externalChannelResponseHandler = externalChannelResponseHandler;

        Arrays.asList(EventType.NEW_NOTIFICATION, EventType.NOTIFICATION_VIEWED);

    }

    //private MyProcessor processor;
    
    /*public PnEventInboundService(@Lazy MyProcessor processor) {
        this.processor = processor;
    }*/

  /*  @Bean
    public Supplier<Flux<Instant>>localdeliverypushproducer(){
        return () -> Flux.interval(Duration.ofSeconds(5)).map(value -> Instant.now()).log();
    }
    */


    /*
    @Bean
    public Consumer<Message<PnDeliveryNewNotificationEvent.Payload>> pnDeliveryEventInboundConsumer() {
        return (message) -> {
            log.debug("New Notification event received");

            PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                    .payload(message.getPayload())
                    .header(mapStandardEventHeader(message.getHeaders()))
                    .build();

            startWorkflowHandler.startWorkflow(pnDeliveryNewNotificationEvent.getHeader().getIun());
        };
    }
*/
    @Bean
    public Consumer<GenericEvent<StandardEventHeader, ?>> pnDeliveryEventInboundConsumer() {
        return (message) -> {
            log.debug("New Notification event received");
            StandardEventHeader header = message.getHeader();

            PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                    .payload(message.getPayload())
                    .header(mapStandardEventHeader(message.getHeaders()))
                    .build();

            startWorkflowHandler.startWorkflow(pnDeliveryNewNotificationEvent.getHeader().getIun());
        };
    }

    //PnDeliveryNotificationViewedEvent
    @Bean
    public Consumer<Message<PnExtChnProgressStatusEventPayload>> pnExtChannelEventInboundConsumer() {
        return (message) -> {


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
    
  /*  @StreamListener(MyProcessor.INPUT)
    public void handlePnExtChnPaperEvent(
            @Payload JsonNode event
    ) {
        log.info("PnExtChnPaperEventInboundService - handlePnExtChnPaperEvent - START");
        log.info("PnExtChnPaperEventInboundService - handlePnExtChnPaperEvent - START");
        log.info("PnExtChnPaperEventInboundService - handlePnExtChnPaperEvent - START");
    }
*/
}
