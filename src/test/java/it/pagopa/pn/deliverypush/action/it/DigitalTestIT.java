package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtilsImpl;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.TimelineDaoMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.awaitility.Awaitility.await;

class DigitalTestIT extends CommonTestConfiguration {
    @SpyBean
    ExternalChannelMock externalChannelMock;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @SpyBean
    CompletionWorkFlowHandler completionWorkflow;
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @SpyBean
    SchedulerService scheduler;
    @Autowired
    PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    StatusUtils statusUtils;
    @Autowired
    PnExternalRegistryClient pnExternalRegistryClient;
    
    @Test
    void completeFailWithRegisteredLetterAlreadyViewedCourtesyEmail() {
        /*
       - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Simulata visualizzazione notifica in fase di SEND_COURTESY_MESSAGE (Ottenuto valorizzando il tax id con TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION)
        */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String iun = TestUtils.getRandomIun();
        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .courtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalFailure(iun, recIndex, timelineService))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        
        int sentPecAttemptNumber = checkAllAttemptsFails(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex);

        //Viene verificato che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow);
        
        //Viene verificato il mancato invio della registered letter, dal momento che la notifica è stata già visualizzata
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene verificato che non sia avvenuto il perfezionamento dal momento che la notifica è stata visualizzata
        TestUtils.checkIsNotPresentRefinement(iun, recIndex, timelineService);
        
        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }
    
    @Test
    void completeFailWithRegisteredLetterAlreadyViewedCourtesyAppIo(){
        /*
       - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Simulata visualizzazione notifica in fase di SEND_COURTESY_MESSAGE (Ottenuto valorizzando il tax id con TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION)
        */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String iun = TestUtils.getRandomIun();
        
        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                EventId.builder()
                .iun(iun)
                .recIndex(0)
                .courtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build());
        
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                new SendMessageResponse().id("1871").result(SendMessageResponse.ResultEnum.SENT_COURTESY)
        );
        
        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalFailure(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        
        int sentPecAttemptNumber = checkAllAttemptsFails(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex);

        //Viene verificato che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato il mancato invio della registered letter, dal momento che la notifica è stata già visualizzata
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene verificato che non sia avvenuto il perfezionamento dal momento che la notifica è stata visualizzata
        TestUtils.checkIsNotPresentRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    @Disabled("Da capire come calcolare il timestamp di scheduled")
    void completeFailWithRegisteredLetter() {
        /*
       - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
        */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationRecipient(recipient)
                .build();


        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalFailureWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        int sentPecAttemptNumber = checkAllAttemptsFails(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkFailDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato l'invio della registered letter
        TestUtils.checkSendRegisteredLetter(recipient, iun, recIndex, paperChannelMock, timelineService);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkIsPresentRefinement(iun, recIndex, timelineService);

        int refinementNumberOfInvocation = 2;   // ora non viene più conteggiato lo scheduling del next action
        TestUtils.checkFailureRefinement(iun, recIndex, refinementNumberOfInvocation, timelineService, scheduler, pnDeliveryPushConfigs);

        Mockito.verify(scheduler, Mockito.times(3)).scheduleEvent(Mockito.eq(iun), Mockito.eq(recIndex), Mockito.any(), Mockito.any(ActionType.class), Mockito.anyString(), Mockito.any());


        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }
    
    @Test
    void completeFailWithoutRegisteredLetter() {
        /*
       - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
        */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() -> Assertions.assertTrue(TestUtils.checkIsPresentDigitalFailureWorkflowAndRefinement(iun, recIndex, timelineService)));

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        int sentPecAttemptNumber = checkAllAttemptsFails(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkFailDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow);
        
        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }


    private int checkAllAttemptsFails(LegalDigitalAddressInt platformAddress, LegalDigitalAddressInt digitalDomicile, LegalDigitalAddressInt pbDigitalAddress, String iun, Integer recIndex) {
        
        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        int sendAttemptMade = 0;
        int sentPecAttemptNumber = 0;
        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        sentPecAttemptNumber +=1;
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sentPecAttemptNumber +=1;
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;
        sentPecAttemptNumber +=1;
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        sentPecAttemptNumber +=1;
        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sentPecAttemptNumber +=1;
        //Viene verificato che il sesto tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(sentPecAttemptNumber).getIun(), digitalAddressesEvents.get(5).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);
        
        return sentPecAttemptNumber +1;
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
    
    @Test
    void emptyFirstSuccessGeneral() {
  /*
       - Platform address vuoto (Ottenuto non valorizzando nessun platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando nessun digitalDomicile del recipient)
       - General presente ed primo invio avvenuto con successo (Ottenuto valorizzando il digital address per il recipient in PUB_REGISTRY_DIGITAL con )
    */

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() -> Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService)));

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        
        int sentPecAttemptNumber = 1;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void emptyFirstSuccessSpecial() throws PnIdConflictException {
  /*
       - Platform address vuoto (Ottenuto non valorizzando nessun platformAddress in addressBookEntry)
       - Special address presente e primo invio con successo (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */

        final LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        final NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        final NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() -> 
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 1;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
        
        //Viene effettuato il check dei legalFacts
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }


    @Test
    void firstSuccessGeneral() {
  /*
       - Platform address presente e primo invio con fallimento (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address presente e primo invio con fallimento (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address presente e primo invio con successo (Ottenuto valorizzando digital address per il recipient in ExternalChannelMock.EXT_CHANNEL_WORKS)
    */
        final LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        final LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        final LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        final NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        final NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        pnDeliveryClientMock.addNotification(notification);
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 3;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void firstSuccessPlatform() {
     /*
       - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la presenza dell'indirizzo di piattaforma
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        
        //Viene verificato che sia stata effettuata una sola chiamata ad external channel
        int sentPecAttemptNumber = 1;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    @Disabled("Fails sometimes to verify")
    void firstSuccessSpecial() {
        /*
       - Platform address presente e primo invio con fallimento (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address presente e primo invio con successo (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 2;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();
        
        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void secondSuccessGeneral() {
       /*
       - Platform address presente fallimento sia primo che secondo tentativo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente fallimento sia primo che secondo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address successo (Ottenuto valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST per il recipient in PUB_REGISTRY_DIGITAL)
    */
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();


        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 6;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        checkExternalChannelSentAttempt(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex, notificationIntsEvents, digitalAddressesEvents);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );


        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkExternalChannelSentAttempt(LegalDigitalAddressInt platformAddress, LegalDigitalAddressInt digitalDomicile, LegalDigitalAddressInt pbDigitalAddress, String iun, Integer recIndex, List<NotificationInt> notificationIntsEvents, List<LegalDigitalAddressInt> digitalAddressesEvents) {
        int sendAttemptMade = 0;

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade+=1;

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(4).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il sesto tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(5).getIun(), digitalAddressesEvents.get(5).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);
    }

    @Test
    void secondSuccessPlatform() {
        /*
       - Platform address presente e fallimento primo tentativo e successo secondo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente e fallimento primo tentativo (Ottenuto non valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 4;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();
        
        checkExternalChannelAttempt(platformAddress, digitalDomicile, pbDigitalAddress, iun, recIndex, notificationIntsEvents, digitalAddressesEvents);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );


        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    private void checkExternalChannelAttempt(LegalDigitalAddressInt platformAddress, LegalDigitalAddressInt digitalDomicile, LegalDigitalAddressInt pbDigitalAddress, String iun, Integer recIndex, List<NotificationInt> notificationIntsEvents, List<LegalDigitalAddressInt> digitalAddressesEvents) {
        int sentAttemptMade = 0;

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il terzo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade+= 1;
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.OK);
    }

    @Test
    void secondSuccessSpecial() {
      /*
       - Platform address presente sia primo che secondo tentativo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente fallimento primo tentativo successo secondo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente fallimento primo tentativo (Ottenuto valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->{
                    Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex, timelineService));
                }
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 5;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        int sentAttemptMade = 0;
        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade += 1;
        
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(4).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService,
                null
        );


        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }
}
