package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

@Slf4j
public class HandleEventUtils {
    private HandleEventUtils() {}

    public static void handleException(MessageHeaders headers, Exception ex) {
        if(headers != null){
            StandardEventHeader standardEventHeader = mapStandardEventHeader(headers);
            log.error("Generic exception for iun={} ex={}", standardEventHeader.getIun(), ex);
        }else {
            log.error("Generic exception ex ", ex);
        }
    }
    
    public static StandardEventHeader mapStandardEventHeader(MessageHeaders headers) {
        if(headers != null){
            return StandardEventHeader.builder()
                    .eventId((String) headers.get(PN_EVENT_HEADER_EVENT_ID))
                    .iun((String) headers.get(PN_EVENT_HEADER_IUN))
                    .eventType((String) headers.get(PN_EVENT_HEADER_EVENT_TYPE))
                    .createdAt(mapInstant(headers.get(PN_EVENT_HEADER_CREATED_AT)))
                    .publisher((String) headers.get(PN_EVENT_HEADER_PUBLISHER))
                    .build();
        } else {
            String msg = "Headers cannot be null in mapStandardEventHeader";
            log.error(msg);
            throw new PnInternalException(msg);            
        }
    }

    private static Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }
}
