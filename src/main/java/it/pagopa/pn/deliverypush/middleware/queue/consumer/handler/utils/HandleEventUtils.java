package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_HANDLEEVENTFAILED;

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
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_HANDLEEVENTFAILED);            
        }
    }

    private static Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }

    public static void addIunAndCorrIdToMdc(String iun, String correlationId) {
        addIunToMdc(iun);
        addCorrelationIdToMdc(correlationId);
    }

    public static void addIunAndRecIndexAndCorrIdToMdc(String iun, Integer recIndex, String correlationId) {
        addIunToMdc(iun);
        addRecIndexToMdc(recIndex);
        addCorrelationIdToMdc(correlationId);
    }

    public static void addIunAndRecIndexToMdc(String iun, int recIndex) {
        addIunToMdc(iun);
        addRecIndexToMdc(recIndex);
    }

    public static void addIunToMdc(String iun) {
        MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
    }

    public static void addRecIndexToMdc(Integer recIndex) {
        if(recIndex != null){
            MDC.put(MDCUtils.MDC_PN_CTX_RECIPIENT_INDEX, String.valueOf(recIndex));
        }
    }

    public static void addCorrelationIdToMdc(String correlationId) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, correlationId);
    }
}
