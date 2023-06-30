package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    @Override
    public PnAuditLogEvent buildAuditLogEvent(String iun, PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments) {
        String logMessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{} - iun={}", logMessage, iun)
                .iun(iun)
                .build();
        logEvent.log();
        return logEvent;
    }
    
    @Override
    public PnAuditLogEvent buildAuditLogEvent(String iun, Integer recIndex, PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments) {
        String logMessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{} - iun={} id={}", logMessage, iun, recIndex)
                .iun(iun)
                .build();
        logEvent.log();
        return logEvent;
    }

}
