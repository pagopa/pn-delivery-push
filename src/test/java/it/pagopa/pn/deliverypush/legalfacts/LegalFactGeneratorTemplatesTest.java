package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@TestPropertySource(properties = "pn.delivery-push.enableTemplatesEngine=false")
class LegalFactGeneratorTemplatesTest {

    @SpyBean
    LegalFactGenerator legalFactGeneratorDocComposition;

    @Test
    void generateNotificationReceivedLegalFact() {
        List<NotificationRecipientInt> recipients = new ArrayList<>();
        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .denomination("denomination_test")
                .digitalDomicile(new LegalDigitalAddressInt())
                .build();
        recipients.add(recipientInt);
        NotificationInt notificationInt = NotificationInt.builder()
                .recipients(recipients)
                .subject("subject_test")
                .sentAt(Instant.now())
                .sender(new NotificationSenderInt())
                .documents(new ArrayList<>())
                .build();

        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationReceivedLegalFact(notificationInt));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateNotificationViewedLegalFact() {
    }

    @Test
    void generatePecDeliveryWorkflowLegalFact() {
    }

    @Test
    void generateAnalogDeliveryFailureWorkflowLegalFact() {
    }

    @Test
    void generateNotificationCancelledLegalFact() {
    }

    @Test
    void generateNotificationAARSubject() {
    }

    @Test
    void generateNotificationAAR() {
    }

    @Test
    void generateNotificationAARBody() {
    }

    @Test
    void generateNotificationAARPECBody() {
    }

    @Test
    void generateNotificationAARForSMS() {
    }

    @Test
    void convertFileMonoToBytes() {
    }
}