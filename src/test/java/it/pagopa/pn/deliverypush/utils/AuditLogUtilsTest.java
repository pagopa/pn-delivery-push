package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditLogUtilsTest {
    
    @Test
    void getAuditLogEventTypeMandate() {
        String iun_01 = "IUN_01";
        String taxId = "TaxId";
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId(taxId+"_ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun_01)
                .withNotificationRecipient(recipient)
                .build();

        PnAuditLogEventType type = AuditLogUtils.getAuditLogEventType(notification, null, "mandate");

        Assertions.assertEquals(PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP, type);
    }

    @Test
    void getAuditLogEventTypeRecipient() {
        String iun_01 = "IUN_01";
        String taxId = "TaxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId(taxId+"_ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun_01)
                .withNotificationRecipient(recipient)
                .build();

        PnAuditLogEventType type = AuditLogUtils.getAuditLogEventType(notification, recipient.getInternalId(), null);

        Assertions.assertEquals(PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP, type);
    }

    @Test
    void getAuditLogEventTypeRecipientSender() {
        String iun_01 = "IUN_01";
        String taxId = "TaxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId(taxId+"_ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun_01)
                .withPaId("paMilano")
                .withNotificationRecipient(recipient)
                .build();

        PnAuditLogEventType type = AuditLogUtils.getAuditLogEventType(notification, notification.getSender().getPaId(), null);

        Assertions.assertEquals(PnAuditLogEventType.AUD_NT_LEGALOPEN_SND, type);
    }

    @Test
    void getAuditLogEventTypeRecipientError() {
        String iun_01 = "IUN_01";
        String taxId = "TaxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId(taxId+"_ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun_01)
                .withPaId("paMilano")
                .withNotificationRecipient(recipient)
                .build();

        assertThrows(PnInternalException.class, () -> {
            AuditLogUtils.getAuditLogEventType(notification, "TO_ERROR", null);
        });

    }
}