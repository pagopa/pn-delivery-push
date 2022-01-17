package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotificationTestBuilder {
    private String iun;
    private List<NotificationRecipient> recipients;

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

    public NotificationTestBuilder withNotificationRecipient(NotificationRecipient recipient) {
        this.recipients = Collections.singletonList(
                recipient
        );
        return this;
    }

    public NotificationTestBuilder withNotificationRecipients(List<NotificationRecipient> recipientCollections) {
        this.recipients = recipientCollections;
        return this;
    }

    public Notification build() {
        return Notification.builder()
                .iun(iun)
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .sentAt(Instant.now())
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(recipients)
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
    }
}
