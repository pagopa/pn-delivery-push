package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;

import java.util.ArrayList;
import java.util.List;


public class NotificationMapper {
    private NotificationMapper(){}

    public static NotificationInt externalToInternal(SentNotification sentNotification) {

        List<NotificationRecipientInt> listNotificationRecipientInt = mapNotificationRecipient(sentNotification.getRecipients());
        List<NotificationDocumentInt> listNotificationDocumentIntInt = mapNotificationDocument(sentNotification.getDocuments());

        return NotificationInt.builder()
                .iun(sentNotification.getIun())
                .paNotificationId(sentNotification.getPaProtocolNumber())
                .sentAt(DateUtils.convertOffsetDateTimeToInstant(sentNotification.getSentAt()))
                .sender(
                        NotificationSenderInt.builder()
                                .paId(sentNotification.getSenderPaId())
                                .paDenomination(sentNotification.getSenderDenomination())
                                .build()
                )
                .documents(listNotificationDocumentIntInt)
                .recipients(listNotificationRecipientInt)
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
            NotificationRecipientInt notificationRecInt = NotificationRecipientInt
                    .builder()
                    .taxId(recipient.getTaxId())
                    .denomination(recipient.getDenomination())
                    .build();

            it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationDigitalAddress digitalDomicile = recipient.getDigitalDomicile();
            if(digitalDomicile != null){
                DigitalAddress.TypeEnum typeEnum = DigitalAddress.TypeEnum.valueOf(digitalDomicile.getType().name());

                notificationRecInt.toBuilder()
                        .digitalDomicile(
                                DigitalAddress.builder()
                                        .address(digitalDomicile.getAddress())
                                        .type(typeEnum)
                                        .build()
                        ).build();
            }

            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            if(physicalAddress != null){
                notificationRecInt.toBuilder()
                        .physicalAddress(
                                PhysicalAddress.builder()
                                        .at(physicalAddress.getAt())
                                        .address(physicalAddress.getAddress())
                                        .addressDetails(physicalAddress.getAddressDetails())
                                        .foreignState(physicalAddress.getForeignState())
                                        .municipality(physicalAddress.getMunicipality())
                                        .municipalityDetails(physicalAddress.getMunicipalityDetails())
                                        .province(physicalAddress.getProvince())
                                        .zip(physicalAddress.getZip())
                                        .build()
                        ).build();
            }

            it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationPaymentInfo payment = recipient.getPayment();

            if(payment != null){
                NotificationPaymentInfoInt paymentInfoInt = NotificationPaymentInfoInt.builder()
                        .pagoPaForm(
                                NotificationDocumentInt.builder()
                                        .digests(
                                                NotificationDocumentInt.Digests.builder()
                                                        .sha256(payment.getPagoPaForm().getDigests().getSha256())
                                                        .build()
                                        )
                                        .ref(
                                                NotificationDocumentInt.Ref.builder()
                                                        .key(payment.getPagoPaForm().getRef().getKey())
                                                        .versionToken(payment.getPagoPaForm().getRef().getVersionToken())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

                if(payment.getF24flatRate() != null){
                    paymentInfoInt.toBuilder()
                            .f24flatRate(
                                    NotificationDocumentInt.builder()
                                            .digests(
                                                    NotificationDocumentInt.Digests.builder()
                                                            .sha256(payment.getF24flatRate().getDigests().getSha256())
                                                            .build()
                                            )
                                            .ref(
                                                    NotificationDocumentInt.Ref.builder()
                                                            .key(payment.getF24flatRate().getRef().getKey())
                                                            .versionToken(payment.getF24flatRate().getRef().getVersionToken())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();
                }
                if(payment.getF24white() != null){
                    paymentInfoInt.toBuilder()
                            .f24white(
                                    NotificationDocumentInt.builder()
                                            .digests(
                                                    NotificationDocumentInt.Digests.builder()
                                                            .sha256(payment.getF24white().getDigests().getSha256())
                                                            .build()
                                            )
                                            .ref(
                                                    NotificationDocumentInt.Ref.builder()
                                                            .key(payment.getF24white().getRef().getKey())
                                                            .versionToken(payment.getF24white().getRef().getVersionToken())
                                                            .build()
                                            )
                                            .build()
                            ).build();
                }

            }
            list.add(notificationRecInt);
        }
        return list;
    }
}
