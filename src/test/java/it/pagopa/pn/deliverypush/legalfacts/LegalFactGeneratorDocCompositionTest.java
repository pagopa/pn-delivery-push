package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties="pn.delivery-push.enableTemplatesEngine=false")
class LegalFactGeneratorDocCompositionTest {

    @SpyBean
    LegalFactGenerator legalFactGeneratorDocComposition;

    @Test
    void generateNotificationReceivedLegalFact() {
        NotificationInt notificationInt = NotificationInt.builder().build();
        Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationReceivedLegalFact(notificationInt));
    }
}