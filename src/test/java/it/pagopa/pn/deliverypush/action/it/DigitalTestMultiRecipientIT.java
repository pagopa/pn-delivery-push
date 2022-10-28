package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        PnAuditLogBuilder.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        DigitalWorkFlowExternalChannelResponseHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistryServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        NotificationCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        NotificationViewedHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        StatusUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        StatusServiceImpl.class,
        AddressBookServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AttachmentUtils.class,
        NotificationUtils.class,
        CompletionWorkflowUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        PnDeliveryPushConfigs.class,
        DigitalTestIT.SpringTestConfiguration.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
class DigitalTestMultiRecipientIT {

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private CompletionWorkFlowHandler completionWorkflow;

    @SpyBean
    private LegalFactGenerator legalFactGenerator;

    @SpyBean
    private SchedulerService scheduler;

    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private InstantNowSupplier instantNowSupplier;

    @Autowired
    private SafeStorageClientMock safeStorageClientMock;

    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private UserAttributesClientMock addressBookMock;

    @Autowired
    private PublicRegistryMock publicRegistryMock;

    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;

    @Autowired
    private NotificationViewedHandler notificationViewedHandler;

    @Autowired
    private ChooseDeliveryModeHandler chooseDeliveryType;

    @Autowired
    private StatusUtils statusUtils;

    @Autowired
    private PnExternalRegistryClient pnExternalRegistryClient;

    @BeforeEach
    public void setup() {

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDeliveryClientMock.clear();
        pnDataVaultClientMock.clear();
        safeStorageClientMock.clear();
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));
        publicRegistryMock.addDigital(recipient2.getTaxId(), pbDigitalAddress2);

        String iun = notification.getIun();
        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia presente il REFINEMENT per entrambi i recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(10)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al primo tentativo 
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        int sendAttemptMade = 0;
        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il primo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2,  timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al primo tentativo
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        sendAttemptMade = 0;
        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con l'indirizzo generale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, pbDigitalAddress2, DigitalAddressSourceInt.GENERAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quinto tentativo sia avvenuto con l'indirizzo generale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, pbDigitalAddress2, DigitalAddressSourceInt.GENERAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, pbDigitalAddress2, timelineService);

        //TODO inserirlo nei test del primo recipient
        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                4,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                6,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    // Il primo destinatario è UNREACHBLE, il secondo è raggiungibile, ma il primo destinatario visualizza la notifica
    // via PN dopo il primo feedback (negativo) di External Channels.
    @Test
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

        String iun = "IUN01";

        //Simulazione visualizzazione notifica a valle della ricezione primo esito fallito di externalChannel
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
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
        publicRegistryMock.addDigital(recipient2.getTaxId(), pbDigitalAddress2);

        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());
        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non sia presente il REFINEMENT per il secondo recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(10)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al primo tentativo 
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il primo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        int sendAttemptMade = 0;
        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il primo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, sendAttemptMade, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2,  timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al primo tentativo
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la disponibilità degli indirizzi per il secondo recipient relativi al secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        sendAttemptMade = 0;
        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con l'indirizzo generale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, pbDigitalAddress2, DigitalAddressSourceInt.GENERAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sendAttemptMade += 1;

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quinto tentativo sia avvenuto con l'indirizzo generale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, sendAttemptMade, pbDigitalAddress2, DigitalAddressSourceInt.GENERAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(pbDigitalAddress2.getAddress(), iun, recIndex2, sendAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow sia abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, pbDigitalAddress2, timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                4,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                6,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    // il primo destinatario è raggiungibile, il secondo è UNREACHBLE
    @Test
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
                .withIun("IUN01")
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
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e completato con successo
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il primo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                2,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                4,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
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
                .withIun("IUN01")
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
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                4,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                4,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    // Entrambi i destinatari sono non raggiungibili, ma il primo visualizza la notifica su PN dopo che il workflow
    // sia completato (in fallimento)
    @Test
    void rec1AllKoRec2AllKoButFirstViewedAfterWorkflow() {
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
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //Simulazione visualizzazione notifica a valle della fine del workflow di fallimento
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun("IUN01")
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(0)
                        .build()
        );
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
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
                .withIun("IUN01")
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
//        await().untilAsserted(() ->
//                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
//        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                4,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                4,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
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
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //Simulazione visualizzazione notifica prima che il workflow sia completato (con fallimento)
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun("IUN01")
                        .recIndex(0)
                        .source(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(0)
                        .build()
        );
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile1)
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
                .withIun("IUN01")
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
//        await().untilAsserted(() ->
//                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
//        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(8)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, platformAddress1, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito negativo
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex1, 2, timelineService, completionWorkflow);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, platformAddress2, DigitalAddressSourceInt.PLATFORM, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(platformAddress2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale e fallito
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 1, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 1, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato per il secondo recipient che il workflow sia fallito
        TestUtils.checkFailDigitalWorkflowMultiRec(iun, recIndex2, 2,  timelineService, completionWorkflow);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                4,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.FAILURE;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                4,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    // Entrambi i destinatari sono raggiungibili e il primo visualizza la notifica su PN dopo che il workflow
    // sia completato (con successo). Successivamente, anche il secondo destinatario visualizza la notifica
    @Test
    void rec1OKRec2OKAndFirstViewedAfterWorkflow() throws InterruptedException {
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

        //Simulazione visualizzazione notifica a valle della fine del workflow di successo
        String taxId01 = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun("IUN01")
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
                        .iun("IUN01")
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
                .withNotificationDocuments(notificationDocumentList)
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        Thread.sleep(5000L);
        //attendo la fine del workflow
        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, digitalDomicile2,  timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                1,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                1,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        int recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        int recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString());

        // Viene atteso fino a che non sia presente il REFINEMENT per entrambi i recipient
        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex1, timelineService))
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(TestUtils.checkIsPresentRefinement(iun, recIndex2, timelineService))
        );

        //CHECK PRIMO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex1, 0, digitalDomicile1, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile1.getAddress(), iun, recIndex1, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il primo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex1, digitalDomicile1, timelineService);

        //CHECK SECONDO RECIPIENT

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il domicilio digitale e con successo
        TestUtils.checkExternalChannelPecSendFromTimeline(iun, recIndex2, 0, digitalDomicile2, DigitalAddressSourceInt.SPECIAL, timelineService);
        checkIsPresentAcceptanceAndDeliveringAttachmentInTimeline(digitalDomicile2.getAddress(), iun, recIndex2, 0, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

        //Viene verificato per il secondo recipient che il workflow abbia avuto esito positivo
        TestUtils.checkSuccessDigitalWorkflowFromTimeline(iun, recIndex2, digitalDomicile2,  timelineService);

        //Viene effettuato il check dei legalFacts generati per il primo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                1,
                generatedLegalFactsInfo,
                endWorkflowStatus,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient

        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus2 = EndWorkflowStatus.SUCCESS;

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                1,
                generatedLegalFactsInfo2,
                endWorkflowStatus2,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
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
