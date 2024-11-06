package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
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
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

class AnalogTestIT extends CommonTestConfiguration{
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @SpyBean
    ExternalChannelMock externalChannelMock;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @SpyBean
    CompletionWorkFlowHandler completionWorkflow;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    StatusUtils statusUtils;
    
    @Test
    void notificationViewedPaPhysicalAddressSend() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e ottenimento indirizzo investigazione
       - Viene visualizzata la notifica in fase d'invio del messaggio di cortesia, questo comporta che nessun invio analogico successivo avvenga
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = TestUtils.getRandomIun();;

        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .sentAttemptMade(0)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        String timelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() -> 
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );

        // aspetto ulteriori 200ms
        await().pollDelay(Duration.ofMillis(200)).untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress, 0, timelineService);

        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene verificato che la notifica sia stata visualizzata e che il costo sia valorizzato
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));
        
        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementOpt.get();
        NotificationViewedDetailsInt detailsInt = (NotificationViewedDetailsInt) timelineElement.getDetails();
        
        Assertions.assertNotNull(detailsInt.getNotificationCost());

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();
        
        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
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
    void notificationViewedNoAnalogSend() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Viene visualizzata la notifica in fase d'invio del messaggio di cortesia, questo comporta che nessun invio analogico avvenga
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
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
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        //Dal momento che l'ultimo elemento di timeline non viene inserito in prossimità della fine del workflow viene utilizzato un delay
        with().pollDelay(5, SECONDS).await().untilAsserted(() ->{
            String timelineId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recIndex(recIndex)
                            .build()
            );
            
            Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent());
        });
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata l'assenza degli invii verso external channel
        TestUtils.checkNotSendPaperToExtChannel(iun, recIndex, 0, timelineService);
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene verificato che la notifica sia stata visualizzata
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs("Notification is PAID but not VIEWED, should check how!");
    }

    @Test
    void completelyUnreachable() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 

     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@works.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogFailureWorkflowAndRefinement(iun, recIndex, timelineService)
                )
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress, 0, timelineService);
        
        //Viene verificato l'effettivo invio delle due notifiche verso paperChannel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));

        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(0)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendAnalogDetailsInt.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());

        SendAnalogDetailsInt sendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals( paPhysicalAddress.getAddress() , sendPaperDetails.getPhysicalAddress().getAddress() );

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
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

    @Test
    void publicSendFailSecondSendSuccessTest() {
  /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un indirizzo che fallirà nell'invio di external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         con invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successo (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_OK)
ì    */

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogSuccessWorkflowAndRefinement(iun, recIndex, timelineService)
                )
        );
        
        //Viene verificato che non sia stato inviato alcun messaggio di cortesia
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress1, 0, timelineService);

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));

        TestUtils.checkSuccessAnalogWorkflow(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
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
    void completelyUnreachableTwoRecipient() {
 /*
  PRIMO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
        
         SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();
        
        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che per i due recipient non si vada in refinement
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogFailureWorkflowAndRefinement(iun, recIndex1, timelineService)
                )
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REFINEMENT.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex2)
                                        .build())).isPresent())
        );

        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient1
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService);

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient2
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex2, listCourtesyAddressRecipient2, timelineService);

        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(listCourtesyAddressRecipient1.size() + listCourtesyAddressRecipient2.size()))
                .sendCourtesyNotification(
                        Mockito.any(NotificationInt.class), 
                        Mockito.any(NotificationRecipientInt.class),
                        Mockito.any(CourtesyDigitalAddressInt.class),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString()
                );

        //Viene verificata la presenza degli indirizzi per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza degli indirizzi per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        
        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA per il rec1
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA per il rec2
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator,
                timelineService,
                null
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
                generatedLegalFactsInfo2,
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


    @Test
    void twoRecipientDigitalDeliveredAnalogUnreachable() {
 /*     PRIMO RECIPIENT
       - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
        
         SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentDigitalSuccessWorkflowAndRefinement(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentAnalogFailureWorkflowAndRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService);

        //Viene verificata la presenza degli indirizzi per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza degli indirizzi per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che il workflow sia stato completato con successo per il primo recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())
        ).isPresent());
        
        //Viene verificato che il workflow sia fallito per il secondo recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());

        //Viene verificato che il recipient 2 risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento per entrambi i recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());

        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                1,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
                generatedLegalFactsInfo2,
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


    @Test
    void twoRecipientAnalogUnreachableDigitalDelivered() {
 /*     PRIMO RECIPIENT
           - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

         SECONDO RECIPIENT
           - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

     */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer rec1Index = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer rec2Index = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentAnalogFailureWorkflowAndRefinement(iun, rec1Index, timelineService))

        );

        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REFINEMENT.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(rec2Index)
                                        .build())).isPresent())

        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);

        //Viene verificato l'effettivo invio delle due notifiche verso paperChannel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(
                Mockito.any(PaperChannelPrepareRequest.class)
        );


        Mockito.verify(paperChannelMock, Mockito.times(2)).send(
                Mockito.any(PaperChannelSendRequest.class)
            );

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        
        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                rec1Index,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator,
                timelineService,
                null
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                rec2Index,
                1,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
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
    void twoRecipientAnalogUnreachableWaitForDigitalDelivered() {
 /*     PRIMO RECIPIENT
           - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

         SECONDO RECIPIENT
           - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
     */
        
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();
        
        String iun = TestUtils.getRandomIun();

        //Simulazione attesa del primo recipient in 
        String elementIdInWait = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .sentAttemptMade(1)
                        .build()
        );

        String elementIdToWait = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(1)
                        .build()
        );
        
        String taxId1 = TimelineDaoMock.SIMULATE_RECIPIENT_WAIT + elementIdInWait + TimelineDaoMock.WAIT_SEPARATOR + elementIdToWait;
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId1)
                .withInternalId("ANON_"+taxId1)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        String taxId2 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId2)
                .withInternalId("ANON_"+taxId2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        Integer rec1Index = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer rec2Index = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        
        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentAnalogFailureWorkflowAndRefinement(iun, rec1Index, timelineService))
        );

        String timelineId2 = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(rec2Index)
                        .build()
        );

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId2).isPresent())
        );
        
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);

        //Viene verificato l'effettivo invio delle due notifiche verso paperChannel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(
                Mockito.any(PaperChannelPrepareRequest.class)
        );


        Mockito.verify(paperChannelMock, Mockito.times(2)).send(
                Mockito.any(PaperChannelSendRequest.class)
        );

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(true)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                rec1Index,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator,
                timelineService,
                null
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                rec2Index,
                1,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }
    @NotNull
    private static List<String> replaceSafeStorageKeyFromListAttachment(List<String> attachments) {
        return attachments.stream().map( attachment -> attachment.replace(SAFE_STORAGE_URL_PREFIX, "")).toList();
    }

}
