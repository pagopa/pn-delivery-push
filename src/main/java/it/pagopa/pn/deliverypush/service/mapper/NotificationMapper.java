package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationAttachmentBodyRef;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationAttachmentDigests;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationDocument;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationRecipientV21;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV21;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;


public class NotificationMapper {
    private NotificationMapper(){}

    public static NotificationInt externalToInternal(SentNotificationV21 sentNotification) {

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
                .documents(listNotificationDocumentIntInt)
                .recipients(listNotificationRecipientInt)
                .notificationFeePolicy(NotificationFeePolicy.fromValue(sentNotification.getNotificationFeePolicy().getValue()))
                .amount(sentNotification.getAmount())
                .group(sentNotification.getGroup())
                .paymentExpirationDate(paymentExpirationDate)
                .pagoPaIntMode(sentNotification.getPagoPaIntMode() != null ? PagoPaIntMode.valueOf(sentNotification.getPagoPaIntMode().getValue()) : null)
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

    private static List<NotificationRecipientInt> mapNotificationRecipient(List<NotificationRecipientV21> recipients) {
        List<NotificationRecipientInt> list = new ArrayList<>();

        for (NotificationRecipientV21 recipient : recipients){
            NotificationRecipientInt recipientInt = RecipientMapper.externalToInternal(recipient);
            list.add(recipientInt);
        }
        
        return list;
    }
    
    //Utilizzata a livello di test
    public static SentNotificationV21 internalToExternal(NotificationInt notification) {
        SentNotificationV21 sentNotification = new SentNotificationV21();

        sentNotification.setIun(notification.getIun());
        sentNotification.setPaProtocolNumber(notification.getPaProtocolNumber());
        sentNotification.setSentAt(notification.getSentAt());
        sentNotification.setSubject(notification.getSubject());
        sentNotification.setAmount(notification.getAmount());
        sentNotification.setPaFee(notification.getPaFee());

        ZonedDateTime time = DateFormatUtils.parseInstantToZonedDateTime(notification.getPaymentExpirationDate());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedString = time.format(formatter);
        sentNotification.setPaymentExpirationDate(formattedString);
        
        if(notification.getPagoPaIntMode() != null){
            sentNotification.setPagoPaIntMode(SentNotificationV21.PagoPaIntModeEnum.valueOf(notification.getPagoPaIntMode().getValue()));
        }
        if( notification.getPhysicalCommunicationType() != null ) {
            sentNotification.setPhysicalCommunicationType(
                    SentNotificationV21.PhysicalCommunicationTypeEnum.valueOf( notification.getPhysicalCommunicationType().name() )
            );
        }

        NotificationSenderInt sender = notification.getSender();
        if( sender != null ) {
            sentNotification.setSenderDenomination( sender.getPaDenomination() );
            sentNotification.setSenderPaId( sender.getPaId() );
            sentNotification.setSenderTaxId( sender.getPaTaxId() );
        }

        List<NotificationRecipientV21> recipients = notification.getRecipients().stream()
                .map(RecipientMapper::internalToExternal).toList();

        sentNotification.setRecipients(recipients);

        List<NotificationDocument> documents = notification.getDocuments().stream().map(
                NotificationMapper::getNotificationDocument).toList();

        sentNotification.setDocuments(documents);

        if(notification.getPhysicalCommunicationType() != null){
            sentNotification.setPhysicalCommunicationType(SentNotificationV21.PhysicalCommunicationTypeEnum.valueOf(notification.getPhysicalCommunicationType().name()));
        }
        
        if(notification.getSender() != null){
            sentNotification.setSenderPaId(notification.getSender().getPaId());
            sentNotification.setSenderDenomination(notification.getSender().getPaDenomination());
            sentNotification.setSenderTaxId(notification.getSender().getPaTaxId());
        }

        if(notification.getNotificationFeePolicy() != null){
            sentNotification.setNotificationFeePolicy(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationFeePolicy.fromValue(notification.getNotificationFeePolicy().getValue()));
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
