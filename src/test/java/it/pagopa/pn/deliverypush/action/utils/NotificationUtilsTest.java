package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class NotificationUtilsTest {

    @Test
    void getRecipientIndexFromInternalId() {
        NotificationInt notificationInt = buildNotification();
        NotificationUtils utils = new NotificationUtils();
        int index = utils.getRecipientIndexFromInternalId(notificationInt, "internalId");
        Assertions.assertEquals(0, index);
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }

    private NotificationInt buildNotification() {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(buildRecipients())
                .build();
    }

    private List<NotificationRecipientInt> buildRecipients() {
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .internalId("internalId")
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();

        return Collections.singletonList(rec1);
    }
}