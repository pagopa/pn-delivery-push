package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotificationTestBuilder {
    private String iun;
    private List<NotificationRecipientInt> recipients;

    public NotificationTestBuilder() {
        recipients = Collections.emptyList();
    }

    public static NotificationTestBuilder builder() {
        return new NotificationTestBuilder();
    }

    public NotificationTestBuilder withIun(String iun) {
        this.iun = iun;
        return this;
    }

    public NotificationTestBuilder withNotificationRecipient(NotificationRecipientInt recipient) {
        this.recipients = Collections.singletonList(
                recipient
        );
        return this;
    }

    public NotificationTestBuilder withNotificationRecipients(List<NotificationRecipientInt> recipientCollections) {
        this.recipients = recipientCollections;
        return this;
    }

    public NotificationInt build() {
        return NotificationInt.builder()
                .iun(iun)
                .paNotificationId("protocol_01")
                .sentAt(Instant.now())
                .physicalCommunicationType(ServiceLevelTypeInt.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .paDenomination(" Una PA di test")
                        .paTaxId("TESTPA02")
                        .build()
                )
                .recipients(recipients)
                .documents(Arrays.asList(
                        NotificationDocumentInt.builder()
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationDocumentInt.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationDocumentInt.builder()
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationDocumentInt.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
    }
}
