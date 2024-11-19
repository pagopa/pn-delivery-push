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
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;

public class AnalogDeceasedTestIT extends CommonTestConfiguration {

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
    StatusUtils statusUtils;

    @Test
    void singleRecipientDeceasedWithNewWorkflowActive() {
  /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)
        Con l'attivazione del workflow di deceduto, ci aspettiamo che la notifica non sia mai perfezionata.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
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
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService)
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
        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));

        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato che NON sia avvenuto il perfezionamento
        TestUtils.checkIsNotPresentRefinement(iun, recIndex, timelineService);

        //Viene verificato che lo stato della notifica sia RETURNED_TO_SENDER
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.RETURNED_TO_SENDER, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

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
    void singleRecipientDeceasedAndThenCancellation() {
    /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
        Arriva un evento di cancellazione dopo l'evento di deceduto, ci aspettiamo che lo stato della notifica
        passi in CANCELLED.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();
        String iun = TestUtils.getRandomIun();
        String taxId =
                TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(0)
                                .build()
                )+TimelineDaoMock.SIMULATE_AFTER_CANCEL_NOTIFICATION;

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId(taxId)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService)
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
        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));

        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato che NON sia avvenuto il perfezionamento
        TestUtils.checkIsNotPresentRefinement(iun, recIndex, timelineService);

        //Viene verificato che lo stato della notifica sia RETURNED_TO_SENDER
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.RETURNED_TO_SENDER, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .notificationCancelled(true)
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
    void singleRecipientDeceasedWithoutNewWorkflowActive() {
  /*
       PRIMO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)

       Ci aspettiamo che la notifica sia perfezionata in quanto il workflow di deceduto non è attivo.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn(null);

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest01")
                .withTaxId("TAXID01")
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

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che non sia stato inviato alcun messaggio di cortesia
        TestUtils.checkSendCourtesyAddresses(iun, recIndex1, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che sia avvenuto il perfezionamento
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        //Viene verificato lo stato della notifica che risulta perfezionata per decorrenza termini
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che non sia presente un evento di deceduto
        await().untilAsserted(() ->
                Assertions.assertFalse(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex1, timelineService)
                )
        );

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress, 0, timelineService);

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo1 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex1,
                0,
                generatedLegalFactsInfo1,
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
    void multiRecipientWithFirstAnalogOkAndSecondDeceased() {
  /*
       PRIMO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato con successo (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_OK)

       SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)

       Ci aspettiamo che la notifica sia comunque perfezionata a causa della riuscita del workflow per il primo recipient.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest1")
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest2")
                .withTaxId("TAXID02")
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che non sia stato inviato alcun messaggio di cortesia per entrambi i recipient
        TestUtils.checkSendCourtesyAddresses(iun, recIndex1, Collections.emptyList(), timelineService, externalChannelMock);
        TestUtils.checkSendCourtesyAddresses(iun, recIndex2, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti per entrambi i recipient
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che lo stato della notifica sia transitato in DELIVERED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkNotificationStatusHistoryContainsDesiredStatus(notification, timelineService, statusUtils, NotificationStatusInt.DELIVERED))
        );

        //Viene verificato che sia avvenuto il perfezionamento per il primo recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        //Viene verificato lo stato della notifica che risulta perfezionata per decorrenza termini
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService)
                )
        );

        //Viene verificato che esista un evento di deceduto per il secondo recipient
        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService, completionWorkflow);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo1 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                0,
                generatedLegalFactsInfo1,
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
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
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
    void multiRecipientWithFirstUnreachableAndSecondDeceased() {
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

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)

       Ci aspettiamo che la notifica sia comunque perfezionata a causa della riuscita del workflow per il primo recipient.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");

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
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest2")
                .withTaxId("TAXID02")
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

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
                                        .recIndex(recIndex1)
                                        .build())).isPresent())
        );


        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient1
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService);

        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(listCourtesyAddressRecipient1.size()))
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

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti per il secondo
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA per il rec1
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);

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

        //Viene verificato che lo stato della notifica sia transitato in irreperibile
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkNotificationStatusHistoryContainsDesiredStatus(notification, timelineService, statusUtils, NotificationStatusInt.UNREACHABLE))
        );

        //Viene verificato che sia avvenuto il perfezionamento per il primo recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        //Viene verificato lo stato della notifica che risulta perfezionata per decorrenza termini in seguito al refinement
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService)
                )
        );

        //Viene verificato che esista un evento di deceduto per il secondo recipient
        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService, completionWorkflow);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo1 = TestUtils.GeneratedLegalFactsInfo.builder()
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
                generatedLegalFactsInfo1,
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
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
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
    void multiRecipientWithBothRecipientsDeceased() {
  /*
       PRIMO RECIPIENT
        - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)
       SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)

       Ci aspettiamo che la notifica non sia perfezionata e finisca in stato RETURNED_TO_SENDER.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest1")
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest2")
                .withTaxId("TAXID02")
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che non sia stato inviato alcun messaggio di cortesia per entrambi i recipient
        TestUtils.checkSendCourtesyAddresses(iun, recIndex1, Collections.emptyList(), timelineService, externalChannelMock);
        TestUtils.checkSendCourtesyAddresses(iun, recIndex2, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti per entrambi i recipient
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che lo stato della notifica sia transitato in RETURNED_TO_SENDER
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.RETURNED_TO_SENDER, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che non sia avvenuto il perfezionamento per entrambi i recipients
        TestUtils.checkIsNotPresentRefinement(iun, recIndex1, timelineService);
        TestUtils.checkIsNotPresentRefinement(iun, recIndex2, timelineService);


        Assertions.assertTrue(
                TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex1, timelineService)
        );
        Assertions.assertTrue(
                TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService)
        );

        //Viene verificato che esista un evento di deceduto per entrambi recipient
        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex1, timelineService, completionWorkflow);
        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService, completionWorkflow);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo1 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                0,
                generatedLegalFactsInfo1,
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
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
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
    void multiRecipientWithFirstAnalogOkAndSecondDeceasedWhichVisualizesTheNotification() {
  /*
       PRIMO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato con successo (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_OK)

       SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente (Ottenuto valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un feedback per una consegna fallita a causa di destinatario deceduto
       (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_DECEASED)

        Triggheriamo la visualizzazione della notifica per il secondo recipient che però non dovrebbe avere effetti sullo stato della notifica.
        Infatti ci aspettiamo che la notifica sia comunque perfezionata per decorrenza termini a causa della riuscita del workflow per il primo recipient.
    */
        Mockito.when(cfg.getActivationDeceasedWorfklowDate()).thenReturn("2021-09-01T00:00:00Z");

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest1")
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_DECEASED)
                .build();

        String iun = TestUtils.getRandomIun();

        // Tramite l'utilizzo della costante TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION ci assicuriamo che la notifica
        // venga visualizzata in teoria anche prima dell'arrivo dell'evento di deceduto per il secondo recipient
        String taxIdRecipient2 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +
                TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(1)
                                .sentAttemptMade(0)
                                .build()
                );

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest2")
                .withTaxId(taxIdRecipient2)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        //Viene verificato che non sia stato inviato alcun messaggio di cortesia per entrambi i recipient
        TestUtils.checkSendCourtesyAddresses(iun, recIndex1, Collections.emptyList(), timelineService, externalChannelMock);
        TestUtils.checkSendCourtesyAddresses(iun, recIndex2, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti per entrambi i recipient
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che lo stato della notifica sia transitato in DELIVERED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkNotificationStatusHistoryContainsDesiredStatus(notification, timelineService, statusUtils, NotificationStatusInt.DELIVERED))
        );

        //Viene verificato che sia avvenuto il perfezionamento per il primo recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        //Viene verificato lo stato della notifica che risulta perfezionata per decorrenza termini
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService)
                )
        );

        //Viene verificato che esista un evento di deceduto per il secondo recipient
        TestUtils.checkAnalogWorkflowRecipientDeceased(iun, recIndex2, timelineService, completionWorkflow);

        //Viene verificato che esista un evento di NOTIFICATION_VIEWED
        await().untilAsserted(() ->
                TestUtils.checkIsPresentViewed(iun, recIndex2, timelineService)
        );

        //Viene verificato che non esista uno stato di VIEWED poichè la visualizzazione da parte di un deceduto non ha effetto
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                Assertions.assertFalse(TestUtils.checkNotificationStatusHistoryContainsDesiredStatus(notification, timelineService, statusUtils, NotificationStatusInt.VIEWED))
        );

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo1 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                0,
                generatedLegalFactsInfo1,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true) //Nonostante la visualizzazione non infici sul calcolo dello stato, viene comunque prodotto il legalfact
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
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

    private void checkNotificationCancelledTimelineElement(String iun) {
        String timelineId = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineService.getTimelineElement(iun, timelineId );

        Assertions.assertTrue(timelineElementInternalOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternalOpt.get();
        Assertions.assertEquals(iun, timelineElement.getIun());
    }
}
