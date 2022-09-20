package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_AUDIT_LOG_FAILED;

@Slf4j
public class AuditLogUtils {
    public static final String EMPTY = "empty";
    public static final String SENSITIVE = "'Sensitive information'";

    private AuditLogUtils() {
    }

    public static PnAuditLogEventType getAuditLogEventType(NotificationInt notification, String senderRecipientId, String mandateId) {
        if (StringUtils.hasText(mandateId)) {
            //La request è stata ricevuta da un delagato, generazione audit log per destinatario
            return PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP;
        } else {
            String paId = notification.getSender().getPaId();
            boolean isRequestFromSender = senderRecipientId.equals(paId);

            //Viene verificato se la richiesta proviene dalla Pa indicata nella notifica
            if (isRequestFromSender) {
                //La request è stata ricevuta dalla PA, generazione audit log per sender
                return PnAuditLogEventType.AUD_NT_LEGALOPEN_SND;
            } else {
                boolean isRequestFromRecipient = notification.getRecipients().stream().anyMatch(
                        recipient -> recipient.getInternalId().equals(senderRecipientId)
                );

                if (isRequestFromRecipient) {
                    //La request è stata ricevuta dal recipient, generazione audit log per recipient
                    return PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP;
                } else {
                    log.error("Request is not from any authorized user. The audit log type cannot be determined - iun={} senderRecipientId={} mandateId={}", notification.getIun(), senderRecipientId, mandateId);
                    throw new PnInternalException("Request is not from any authorized user. The audit log type cannot be determined - iun=" + notification.getIun() + " senderRecipientId=" + senderRecipientId + " mandateId=" + mandateId, ERROR_CODE_AUDIT_LOG_FAILED);
                }
            }
        }
    }

}
