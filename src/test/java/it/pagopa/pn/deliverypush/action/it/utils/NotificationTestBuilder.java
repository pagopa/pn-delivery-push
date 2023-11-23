package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.util.Base64Utils;

public class NotificationTestBuilder {
    private String iun;
    private String paId;
    private List<NotificationRecipientInt> recipients;
    private NotificationFeePolicy notificationFeePolicy;
    private Instant sentAt;
    private List<NotificationDocumentInt> notificationDocument;
    private PagoPaIntMode pagoPaIntMode;
    private Integer paFee;

    private String group;
    
    public NotificationTestBuilder() {
        sentAt = Instant.now();
        recipients = Collections.emptyList();
        notificationDocument = Collections.emptyList();
    }

    public static NotificationTestBuilder builder() {
        return new NotificationTestBuilder();
    }
    
    public NotificationTestBuilder withIun(String iun) {
        this.iun = iun;
        return this;
    }

    public NotificationTestBuilder withPaId(String paId) {
        this.paId = paId;
        return this;
    }

    public NotificationTestBuilder withNotificationFeePolicy(NotificationFeePolicy notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
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

    public NotificationTestBuilder withSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public NotificationTestBuilder withNotificationDocuments(List<NotificationDocumentInt> documents) {
        this.notificationDocument = documents;
        return this;
    }

    public NotificationTestBuilder withPagoPaIntMode(PagoPaIntMode pagoPaIntMode) {
        this.pagoPaIntMode = pagoPaIntMode;
        return this;
    }

    public NotificationTestBuilder withPaFee(int paFee) {
        this.paFee = paFee;
        return this;
    }

    public NotificationTestBuilder withGroup(String group1) {
        this.group = group1;
        return this;
    }

    public NotificationInt build() {
        if(iun == null){
            iun = TestUtils.getRandomIun(4);
        }
        
        if(paId == null){
            paId = "generatedPaId";
        }
        
        if( notificationDocument.isEmpty() ){
            String fileDoc = "sha256_doc00";

            notificationDocument = List.of(
                    NotificationDocumentInt.builder()
                            .ref(NotificationDocumentInt.Ref.builder()
                                    .key(Base64Utils.encodeToString(fileDoc.getBytes()))
                                    .versionToken("v01_doc00")
                                    .build()
                            )
                            .digests(NotificationDocumentInt.Digests.builder()
                                    .sha256(Base64Utils.encodeToString(fileDoc.getBytes()))
                                    .build()
                            )
                            .build()
            );
        }

        if(notificationFeePolicy == null) {
            notificationFeePolicy = NotificationFeePolicy.FLAT_RATE;
        }

        if(pagoPaIntMode == null) {
            pagoPaIntMode = PagoPaIntMode.SYNC;
        }
        
        return NotificationInt.builder()
                .iun(iun)
                .paProtocolNumber("protocol_01")
                .subject("subject not very long but not too short")
                .sentAt(Instant.now())
                .amount(18)
                .paymentExpirationDate(DateFormatUtils.parseDate("2002-08-12").toInstant())
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER)
                .sender(NotificationSenderInt.builder()
                        .paId(paId)
                        .paDenomination("Denominazione pa con id " + paId)
                        .paTaxId("CFPA-" + paId)
                        .build()
                )
                .notificationFeePolicy(notificationFeePolicy)
                .sentAt( sentAt )
                .recipients(recipients)
                .documents(notificationDocument)
                .pagoPaIntMode(pagoPaIntMode)
                .paFee(paFee)
                .group(group)
                .build();
    }

}
