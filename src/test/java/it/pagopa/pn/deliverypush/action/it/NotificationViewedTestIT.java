package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.RecipientType;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;

class NotificationViewedTestIT extends CommonTestConfiguration{
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    NotificationViewedRequestHandler notificationViewedRequestHandler;
    @Autowired
    StatusUtils statusUtils;
    @SpyBean
    SaveLegalFactsService legalFactStore;
    @SpyBean
    PaperNotificationFailedService paperNotificationFailedService;
    @SpyBean
    TimelineService timelineService;
    
    @Test
    @Disabled("unpredictable behavior")
    void notificationViewedFromDelegate() {
        //GIVEN
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //OK
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //ok
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .withNotificationDocuments(notificationDocumentList)
                .build();


        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String delegateInternalId = "delegateInternalId";
        RecipientType delegateType = RecipientType.PF;

        BaseRecipientDto baseRecipientDto = BaseRecipientDto.builder()
                .internalId(delegateInternalId)
                .denomination("delegateName")
                .taxId("delegateTaxId")
                .recipientType(delegateType)
                .build();

        pnDataVaultClientReactiveMock.insertBaseRecipientDto(baseRecipientDto);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Simulazione visualizzazione della notifica
        Instant notificationViewDate = Instant.now();

        DelegateInfoInt delegateInfoInt = DelegateInfoInt.builder()
                .internalId(delegateInternalId)
                .mandateId("delegateMandateId")
                .operatorUuid("delegateOperator")
                .delegateType(RecipientTypeInt.valueOf(delegateType.getValue()))
                .build();

        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, delegateInfoInt, notificationViewDate);

        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.VIEWED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        delegateInfoInt.setDenomination(baseRecipientDto.getDenomination());
        delegateInfoInt.setTaxId(baseRecipientDto.getTaxId());

        checkNotificationViewTimelineElement(iun, recIndex, notificationViewDate, delegateInfoInt);
        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification), eq(recipient), eq(delegateInfoInt), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getInternalId(), iun);

        //Simulazione seconda visualizzazione della notifica
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, delegateInfoInt, Instant.now());

        //Viene effettuata la verifica che i processi correlati alla visualizzazione non siano avvenuti, dunque che il numero d'invocazioni dei metodi sia rimasto lo stesso
        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification),eq(recipient), eq(delegateInfoInt), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getInternalId(), iun);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                6,
                generatedLegalFactsInfo,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator,
                timelineService,
                delegateInfoInt
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Disabled("fail only in build fase")
    void notificationViewed(){
        //GIVEN
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //OK
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //ok
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .withNotificationDocuments(notificationDocumentList)
                .build();


        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Simulazione visualizzazione della notifica
        Instant notificationViewDate = Instant.now();
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, null, notificationViewDate);

        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.VIEWED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        String internalId = recipient.getInternalId();
        
        await().untilAsserted(() -> 
                Assertions.assertThrows(PnNotFoundException.class, () -> paperNotificationFailedService.getPaperNotificationByRecipientId(internalId, false))
        );
        
        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkNotificationViewTimelineElement(iun, recIndex, notificationViewDate, null);
        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification), eq(recipient), eq(null), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getInternalId(), iun);

        //Simulazione seconda visualizzazione della notifica
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex, null, Instant.now());

        checkIsNotificationViewed(iun, recIndex, notificationViewDate);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione non siano avvenuti, dunque che il numero d'invocazioni dei metodi sia rimasto lo stesso
        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification),eq(recipient), eq(null), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getInternalId(), iun);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                6,
                generatedLegalFactsInfo,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator,
                timelineService,
                null
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkIsNotificationViewed(String iun, Integer recIndex, Instant notificationViewDate) {
        Optional<TimelineElementInternal> notificationViewTimelineElementOpt = timelineService.getTimelineElement(iun, TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        ));

        Assertions.assertTrue(notificationViewTimelineElementOpt.isPresent());
        TimelineElementInternal notificationViewTimelineElement = notificationViewTimelineElementOpt.get();
        Assertions.assertEquals(notificationViewDate, ((NotificationViewedDetailsInt)notificationViewTimelineElement.getDetails()).getEventTimestamp());
    }

    private void checkNotificationViewTimelineElement(String iun,
                                                      Integer recIndex,
                                                      Instant notificationViewDate,
                                                      DelegateInfoInt delegateInfo) {
        String timelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineService.getTimelineElement(iun, timelineId );

        Assertions.assertTrue(timelineElementInternalOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternalOpt.get();
        Assertions.assertEquals(iun, timelineElement.getIun());
        Assertions.assertEquals(notificationViewDate, ((NotificationViewedDetailsInt)timelineElement.getDetails()).getEventTimestamp());

        NotificationViewedDetailsInt details = (NotificationViewedDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals(recIndex, details.getRecIndex());
        Assertions.assertEquals(delegateInfo, details.getDelegateInfo());
    }

    @Test
    void testNotificationViewedTwoRecipient(){
        //Primo Recipient
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId(taxId01 +"anon")
                .withDigitalDomicile(digitalDomicile1)
                .build();

        //Secondo recipient
        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId(taxId02 + "ANON")
                .withDigitalDomicile(digitalDomicile2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);


        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(recipients)
                .build();
        
        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in ACCEPTED
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.ACCEPTED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Simulazione visualizzazione della notifica per il primo recipient
        Instant notificationViewDate1 = Instant.now();
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex1, null, notificationViewDate1);

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        timelineService.getTimelineElement(iun, TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex1)
                                        .build()
                        )).isPresent()
                )
        );
        
        checkIsNotificationViewed(iun, recIndex1, notificationViewDate1);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkNotificationViewTimelineElement(iun, recIndex1, notificationViewDate1, null);

        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification),eq(recipient1), Mockito.eq(null), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient1.getInternalId(), iun);

        //Simulazione visualizzazione della notifica per il secondo recipient
        Instant notificationViewDate2 = Instant.now();
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recIndex2, null, notificationViewDate2);


        await().untilAsserted(() ->
                Assertions.assertTrue(
                        timelineService.getTimelineElement(iun, TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex2)
                                        .build()
                        )).isPresent()
                )
        );
        
        checkIsNotificationViewed(iun, recIndex2, notificationViewDate2);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkNotificationViewTimelineElement(iun, recIndex2, notificationViewDate2, null);

        Mockito.verify(legalFactStore, Mockito.times(1)).sendCreationRequestForNotificationViewedLegalFact(eq(notification),eq(recipient2), eq(null), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient2.getInternalId(), iun);
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }
}
