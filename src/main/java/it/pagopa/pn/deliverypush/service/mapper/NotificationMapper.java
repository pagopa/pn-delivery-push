package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class NotificationMapper {
    private NotificationMapper(){}

    public static NotificationInt externalToInternal(SentNotification sentNotification) {

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
                .documents(listNotificationDocumentIntInt)
                .recipients(listNotificationRecipientInt)
                .amount(sentNotification.getAmount())
                .paymentExpirationDate(paymentExpirationDate)
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

    private static List<NotificationRecipientInt> mapNotificationRecipient(List<NotificationRecipient> recipients) {
        List<NotificationRecipientInt> list = new ArrayList<>();

        for (NotificationRecipient recipient : recipients){
            NotificationRecipientInt recipientInt = RecipientMapper.externalToInternal(recipient);
            list.add(recipientInt);
        }
        
        return list;
    }
    
    //Utilizzata a livello di test
    public static SentNotification internalToExternal(NotificationInt notification) {
        SentNotification sentNotification = new SentNotification();

        sentNotification.setIun(notification.getIun());
        sentNotification.setPaProtocolNumber(notification.getPaProtocolNumber());
        sentNotification.setSentAt(notification.getSentAt());
        sentNotification.setSubject(notification.getSubject());
        sentNotification.setAmount(notification.getAmount());
        
        ZonedDateTime time = DateFormatUtils.parseInstantToZonedDateTime(notification.getPaymentExpirationDate());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedString = time.format(formatter);
        sentNotification.setPaymentExpirationDate(formattedString);
        
        if( notification.getPhysicalCommunicationType() != null ) {
            sentNotification.setPhysicalCommunicationType(
                    SentNotification.PhysicalCommunicationTypeEnum.valueOf( notification.getPhysicalCommunicationType().name() )
            );
        }

        NotificationSenderInt sender = notification.getSender();
        if( sender != null ) {
            sentNotification.setSenderDenomination( sender.getPaDenomination() );
            sentNotification.setSenderPaId( sender.getPaId() );
            sentNotification.setSenderTaxId( sender.getPaTaxId() );
        }

        List<NotificationRecipient> recipients = notification.getRecipients().stream()
                .map(RecipientMapper::internalToExternal).toList();

        sentNotification.setRecipients(recipients);

        List<NotificationDocument> documents = notification.getDocuments().stream().map(
                NotificationMapper::getNotificationDocument).toList();

        sentNotification.setDocuments(documents);

        if(notification.getPhysicalCommunicationType() != null){
            sentNotification.setPhysicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.valueOf(notification.getPhysicalCommunicationType().name()));
        }
        
        if(notification.getSender() != null){
            sentNotification.setSenderPaId(notification.getSender().getPaId());
            sentNotification.setSenderDenomination(notification.getSender().getPaDenomination());
            sentNotification.setSenderTaxId(notification.getSender().getPaTaxId());
        }
        
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
