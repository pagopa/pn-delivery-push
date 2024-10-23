package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class NotificationMapper {
    private NotificationMapper(){}

    public static NotificationInt externalToInternal(SentNotificationV24 sentNotification) {

        List<NotificationRecipientInt> listNotificationRecipientInt = mapNotificationRecipient(sentNotification.getRecipients());
        List<NotificationDocumentInt> listNotificationDocumentIntInt = mapNotificationDocument(sentNotification.getDocuments());

        ServiceLevelTypeInt lvl =  ServiceLevelTypeInt.valueOf( sentNotification.getPhysicalCommunicationType().name());
        
        Instant paymentExpirationDate = null;
        if( sentNotification.getPaymentExpirationDate() != null ){
            ZonedDateTime dateTime = DateFormatUtils.parseDate(sentNotification.getPaymentExpirationDate());
            paymentExpirationDate = dateTime.toInstant();
        }
        
        return NotificationInt.builder()
                .iun(sentNotification.getIun())
                .subject(sentNotification.getSubject())
                .paProtocolNumber(sentNotification.getPaProtocolNumber())
                .physicalCommunicationType( lvl )
                .sentAt(sentNotification.getSentAt())
                .sender(
                        NotificationSenderInt.builder()
                                .paTaxId( sentNotification.getSenderTaxId() )
                                .paId(sentNotification.getSenderPaId())
                                .paDenomination(sentNotification.getSenderDenomination())
                                .build()
                )
                .paFee(sentNotification.getPaFee())
                .vat(sentNotification.getVat())
                .documents(listNotificationDocumentIntInt)
                .recipients(listNotificationRecipientInt)
                .notificationFeePolicy(NotificationFeePolicy.fromValue(sentNotification.getNotificationFeePolicy().getValue()))
                .amount(sentNotification.getAmount())
                .group(sentNotification.getGroup())
                .paymentExpirationDate(paymentExpirationDate)
                .pagoPaIntMode(sentNotification.getPagoPaIntMode() != null ? PagoPaIntMode.valueOf(sentNotification.getPagoPaIntMode().getValue()) : null)
                .version(sentNotification.getVersion())
                .additionalLang(sentNotification.getAdditionalLanguages())
                .build();
    }

    private static List<NotificationDocumentInt> mapNotificationDocument(List<NotificationDocument> documents) {
        List<NotificationDocumentInt> list = new ArrayList<>();

        for (NotificationDocument document : documents){
            NotificationDocumentInt notificationDocumentInt = NotificationDocumentInt.builder()
                    .digests(
                            NotificationDocumentInt.Digests.builder()
                                    .sha256(document.getDigests().getSha256())
                                    .build()
                    )
                    .ref(
                            NotificationDocumentInt.Ref.builder()
                                    .key(document.getRef().getKey())
                                    .versionToken(document.getRef().getVersionToken())
                                    .build()
                    )
                    .build();

            list.add(notificationDocumentInt);
        }

        return list;
    }

    private static List<NotificationRecipientInt> mapNotificationRecipient(List<NotificationRecipientV23> recipients) {
        List<NotificationRecipientInt> list = new ArrayList<>();

        for (NotificationRecipientV23 recipient : recipients){
            NotificationRecipientInt recipientInt = RecipientMapper.externalToInternal(recipient);
            list.add(recipientInt);
        }
        
        return list;
    }
    
    //Utilizzata a livello di test
    public static SentNotificationV24 internalToExternal(NotificationInt notification) {
        SentNotificationV24 sentNotification = new SentNotificationV24();

        sentNotification.setIun(notification.getIun());
        sentNotification.setPaProtocolNumber(notification.getPaProtocolNumber());
        sentNotification.setSentAt(notification.getSentAt());
        sentNotification.setSubject(notification.getSubject());
        sentNotification.setAmount(notification.getAmount());
        sentNotification.setPaFee(notification.getPaFee());
        sentNotification.setVat(notification.getVat());
        sentNotification.setAdditionalLanguages(notification.getAdditionalLang());

        ZonedDateTime time = DateFormatUtils.parseInstantToZonedDateTime(notification.getPaymentExpirationDate());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedString = time.format(formatter);
        sentNotification.setPaymentExpirationDate(formattedString);
        
        if(notification.getPagoPaIntMode() != null){
            sentNotification.setPagoPaIntMode(SentNotificationV24.PagoPaIntModeEnum.valueOf(notification.getPagoPaIntMode().getValue()));
        }
        if( notification.getPhysicalCommunicationType() != null ) {
            sentNotification.setPhysicalCommunicationType(
                    SentNotificationV24.PhysicalCommunicationTypeEnum.valueOf( notification.getPhysicalCommunicationType().name() )
            );
        }

        NotificationSenderInt sender = notification.getSender();
        if( sender != null ) {
            sentNotification.setSenderDenomination( sender.getPaDenomination() );
            sentNotification.setSenderPaId( sender.getPaId() );
            sentNotification.setSenderTaxId( sender.getPaTaxId() );
        }

        List<NotificationRecipientV23> recipients = notification.getRecipients().stream()
                .map(RecipientMapper::internalToExternal).toList();

        sentNotification.setRecipients(recipients);

        List<NotificationDocument> documents = notification.getDocuments().stream().map(
                NotificationMapper::getNotificationDocument).toList();

        sentNotification.setDocuments(documents);

        if(notification.getPhysicalCommunicationType() != null){
            sentNotification.setPhysicalCommunicationType(SentNotificationV24.PhysicalCommunicationTypeEnum.valueOf(notification.getPhysicalCommunicationType().name()));
        }
        
        if(notification.getSender() != null){
            sentNotification.setSenderPaId(notification.getSender().getPaId());
            sentNotification.setSenderDenomination(notification.getSender().getPaDenomination());
            sentNotification.setSenderTaxId(notification.getSender().getPaTaxId());
        }

        if(notification.getNotificationFeePolicy() != null){
            sentNotification.setNotificationFeePolicy(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationFeePolicy.fromValue(notification.getNotificationFeePolicy().getValue()));
        }

        sentNotification.setVersion(notification.getVersion());
        
        return sentNotification;
    }

    @NotNull
    private static NotificationDocument getNotificationDocument(NotificationDocumentInt documentInt) {
        NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
        digests.setSha256(documentInt.getDigests().getSha256());

        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey(documentInt.getRef().getKey());
        ref.setVersionToken(documentInt.getRef().getVersionToken());

        NotificationDocument document = new NotificationDocument();
        document.setDigests(digests);
        document.setRef(ref);
        return document;
    }
    
}
