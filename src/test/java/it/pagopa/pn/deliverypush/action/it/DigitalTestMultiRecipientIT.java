package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.TimelineDaoMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;

@Slf4j
class DigitalTestMultiRecipientIT extends CommonTestConfiguration {
    @SpyBean
    ExternalChannelMock externalChannelMock;
    @SpyBean
    CompletionWorkFlowHandler completionWorkflow;
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationUtils notificationUtils;

    @AfterEach
    public void afterEach(){
        ConsoleAppenderCustom.checkLogs();
    }
    
    // Il primo destinatario è UNREACHBLE, il secondo è raggiungibile
    @Test
    void rec1FailRec2GeneralOk() {
       /* Primo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento 
       - General address presente e secondo tentativo ok
    */

        //Primo Recipient
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        //Secondo recipient
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova2")
                .build();

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress2 = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));
        nationalRegistriesClientMock.addDigital(recipient2.getTaxId(), pbDigitalAddress2);

        String iun = notification.getIun();
        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia completato il workflow per entrambi i recipient
        waitEndWorkflow(iun, recIndex1, recIndex2);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(10)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT
        checkAddressAvailabilityFirstRecipient(iun, recIndex1);

        int sendAttemptMade = 0;
        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il primo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 1,  timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al primo tentativo
        checkAddressAvailabilitySecondRecipient(iun, recIndex2);

        sendAttemptMade = 0;
        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con l'indirizzo generale e fallito
        checkPecSendAndDeliveryAttachment(pbDigitalAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quinto tentativo sia avvenuto con l'indirizzo generale e fallito
        checkPecSendAndDeliveryAttachment(pbDigitalAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, pbDigitalAddress2, timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, false, true, endWorkflowStatus, 4);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 6);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkAddressAvailabilityFirstRecipient(String iun, int recIndex1) {
        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al primo tentativo 
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
    }

    private void waitEndWorkflow(String iun, int recIndex1, int recIndex2) {
        try {
            await().untilAsserted(() -> Assertions.assertTrue(
                    TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex1, timelineService) || 
                            TestUtils.checkIsPresentDigitalFailure(iun, recIndex1, timelineService)
            ));

            await().untilAsserted(() -> Assertions.assertTrue(
                    TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex2, timelineService) ||
                            TestUtils.checkIsPresentDigitalFailure(iun, recIndex2, timelineService)
            ));
        }catch (Exception ex){
            log.error("There aren't refinement, this is the timeline={}",timelineService.getTimeline(iun, true));
        }
        
        await().atLeast(Duration.ofSeconds(1));
    }

    private void checkGeneratedLegalFacts(NotificationRecipientInt recipient1, NotificationInt notification, int recIndex1, boolean isNotificationReceivedLegalFactsGenerated, boolean isNotificationAARGenerated, boolean isNotificationViewedLegalFactGenerated, boolean isPecDeliveryWorkflowLegalFactsGenerated, EndWorkflowStatus endWorkflowStatus, int i) {
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(isNotificationReceivedLegalFactsGenerated)
                .notificationAARGenerated(isNotificationAARGenerated)
                .notificationViewedLegalFactGenerated(isNotificationViewedLegalFactGenerated)
                .pecDeliveryWorkflowLegalFactsGenerated(isPecDeliveryWorkflowLegalFactsGenerated)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                i,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );
    }

    private void checkPecSendAndDeliveryAttachment(LegalDigitalAddressInt platformAddress1, String iun, int recIndex1, int sendAttemptMade, DigitalAddressSourceInt platform, ResponseStatusInt ko) {
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, platformAddress1, platform, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, sendAttemptMade, platform, ko);
    }

    // Il primo destinatario è UNREACHBLE, il secondo è raggiungibile, ma il primo destinatario visualizza la notifica
    // via PN dopo il primo feedback (negativo) di External Channels.
    @Test
    @Disabled("unpredictable behavior")
    void rec1ViewedRec2GeneralOk() { 
       /* Primo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento 
       - General address presente e secondo tentativo ok
    */

        //Primo Recipient
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String iun = TestUtils.getRandomIun();

        //Simulazione visualizzazione notifica a valle della ricezione primo esito fallito di externalChannel
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.PLATFORM)
                        .isFirstSendRetry(false)
                        .sentAttemptMade(0)
                        .build()
        );

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress( ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        //Secondo recipient
        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress2 = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
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
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));
        nationalRegistriesClientMock.addDigital(recipient2.getTaxId(), pbDigitalAddress2);

        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());
        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
            Assertions.assertTrue(TestUtils.checkIsPresentViewed(iun, recIndex1, timelineService) && TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(10)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al primo tentativo 
        checkAddressAvailabilityFirstRecipient(iun, recIndex1);

        int sendAttemptMade = 0;
        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il primo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 1,  timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al primo tentativo
        checkAddressAvailabilitySecondRecipient(iun, recIndex2);

        sendAttemptMade = 0;
        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con l'indirizzo generale e fallito
        checkPecSendAndDeliveryAttachment(pbDigitalAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quinto tentativo sia avvenuto con l'indirizzo generale e fallito
        checkPecSendAndDeliveryAttachment(pbDigitalAddress2, iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow sia abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, pbDigitalAddress2, timelineService);
        
        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, true, true, endWorkflowStatus, 4);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 6);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkAddressAvailabilitySecondRecipient(String iun, int recIndex2) {
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
    }

    // il primo destinatario è raggiungibile, il secondo è UNREACHBLE
    @Test
    @Disabled("unpredictable behavior")
    void rec1PlatformOkRec2AllKo() {
       /* Primo recipient
       - Platform address presente e primo invio con fallimento
       - Special address presente e primo invio con successo
       - General address vuoto
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento 
       - General address vuoto
    */

        //Primo Recipient
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

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
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        //Secondo recipient
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova2")
                .build();

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        String iun = notification.getIun();
        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia presente il REFINEMENT per entrambi i recipient
        waitEndWorkflow(iun, recIndex1, recIndex2);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e completato con successo
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il primo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 1,  timelineService, completionWorkflow);
        
        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, false, true, endWorkflowStatus, 2);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 4);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    // Entrambi i destinatari sono non raggiungibili
    @Test
    void rec1AllKoRec2AllKo() {
       /* Primo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto

       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
    */

        //Primo Recipient

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        //Secondo recipient
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova2")
                .build();

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        String iun = notification.getIun();
        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia presente il REFINEMENT per entrambi i recipient
        waitEndWorkflow(iun, recIndex1, recIndex2);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, false, true, endWorkflowStatus, 4);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 4);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    // Entrambi i destinatari sono non raggiungibili, ma il primo visualizza la notifica su PN dopo che il workflow
    // sia completato (in fallimento)
    @Test
    void rec1AllKoRec2AllKoButFirstViewedAfterWorkflow() {
       /* Primo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
       - Viene però visualizzata la notifica a valle del workflow fallito
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
    */

        //Primo Recipient
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String iun = TestUtils.getRandomIun();

        //Simulazione visualizzazione notifica a valle della fine del workflow di fallimento
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(0)
                        .build()
        );
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        //Secondo recipient
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova2")
                .build();

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia presente il Viewed per il primo recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentViewed(iun, recIndex1, timelineService))
       );

        //Viene atteso fino a che l'ultimo elemento di timeline utile non sia stato inserito
        await().untilAsserted(() -> Assertions.assertTrue(
                TestUtils.checkIsPresentDigitalFailureWorkflowAndRefinement(iun, recIndex2, timelineService)
        ));
        
        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, true, true, endWorkflowStatus, 4);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 4);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    // Entrambi i destinatari sono non raggiungibili, ma il primo visualizza la notifica su PN prima che il workflow
    // sia completato (in fallimento)
    @Test
    void rec1AllKoRec2AllKoButFirstViewedBeforeWorkflow() {
       /* Primo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto

       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente ed entrambi gli invii con fallimento
       - General address vuoto
    */

        //Primo Recipient
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        final String iun = TestUtils.getRandomIun();

        //Simulazione visualizzazione notifica prima che il workflow sia completato (con fallimento)
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.PLATFORM)
                        .isFirstSendRetry(false)
                        .sentAttemptMade(0)
                        .build()
        );
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        //Secondo recipient
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova2")
                .build();

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene atteso fino a che l'ultimo elemento di timeline utile non sia stato inserito
        await().untilAsserted(() -> Assertions.assertTrue(
                TestUtils.checkIsPresentDigitalFailure(iun, recIndex1, timelineService)
        ));

        //Viene atteso fino a che l'ultimo elemento di timeline utile non sia stato inserito
        await().untilAsserted(() -> Assertions.assertTrue(
                TestUtils.checkIsPresentDigitalFailureWorkflowAndRefinement(iun, recIndex2, timelineService)
        ));

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress1, iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        checkPecSendAndDeliveryAttachment(platformAddress2, iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, true, true, endWorkflowStatus, 4);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 4);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    // Entrambi i destinatari sono raggiungibili e il primo visualizza la notifica su PN dopo che il workflow
    // sia completato (con successo). Successivamente, anche il secondo destinatario visualizza la notifica
    @Test
    void rec1OKRec2OKAndFirstViewedAfterWorkflow() {
       /* Primo recipient
       - Platform address non presente
       - Special address presente e primo invio con successo
       - General address vuoto

       Secondo recipient
       - Platform address non presente
       - Special address presente e primo invio con successo
       - General address vuoto
    */

        //Primo Recipient
        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String iun = TestUtils.getRandomIun();
        
        //Simulazione visualizzazione notifica a valle della fine del workflow di successo
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.SPECIAL)
                        .sentAttemptMade(0)
                        .build()
        );
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .build();

        //Secondo recipient
        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //Simulazione visualizzazione notifica a valle della fine del workflow di successo
        String taxId02 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(1)
                        .source(DigitalAddressSourceInt.SPECIAL)
                        .sentAttemptMade(0)
                        .build()
        );
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
                .withDigitalDomicile(digitalDomicile2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentViewed(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentViewed(iun, recIndex2, timelineService))
        );
        
        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        //attendo la fine del workflow del primo destinatario
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService))
        );
        //attendo la fine del workflow del secondo destinatario
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, digitalDomicile2, timelineService))
        );


        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, true, true, endWorkflowStatus, 1);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, true, true, endWorkflowStatus2, 1);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    // Entrambi i destinatari sono raggiungibili ma nessuno visualizza la notifica
    @Test
    void rec1OKRec2OKAndNobodyViewed() {
       /* Primo recipient
       - Platform address non presente
       - Special address presente e primo invio con successo
       - General address vuoto

       Secondo recipient
       - Platform address non presente
       - Special address presente e primo invio con successo
       - General address vuoto
    */

        //Primo Recipient
        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAX01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
                .build();

        //Secondo recipient
        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId02 = "TAX02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId("ANON_"+taxId02)
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
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        int recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia completato il workflow per entrambi i recipient
        waitEndWorkflow(iun, recIndex1, recIndex2);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        
        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        checkPecSendAndDeliveryAttachment(digitalDomicile1, iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        checkPecSendAndDeliveryAttachment(digitalDomicile2, iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, digitalDomicile2,  timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient1, notification, recIndex1, true, true, false, true, endWorkflowStatus, 1);

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;
        checkGeneratedLegalFacts(recipient2, notification, recIndex2, true, true, false, true, endWorkflowStatus2, 1);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(String address,
                                                                           String iun,
                                                                           Integer recIndex,
                                                                           int sendAttemptMade,
                                                                           DigitalAddressSourceInt platform,
                                                                           ResponseStatusInt status) {
        LegalDigitalAddressInt digitalAddress = LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .address(address)
                .build();

        TestUtils.checkIsPresentAcceptanceInTimeline(iun, recIndex, sendAttemptMade, digitalAddress, platform, timelineService);
        TestUtils.checkIsPresentDigitalFeedbackInTimeline(iun, recIndex, sendAttemptMade, digitalAddress, platform, timelineService, status);
    }
}
