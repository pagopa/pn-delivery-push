package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogDeliveryFailureWorkflowLegalFactsGenerator;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationCost;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.notificationview.ViewNotification;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.*;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.*;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.*;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        DigitalWorkFlowExternalChannelResponseHandler.class,
        AnalogFailureDeliveryCreationResponseHandler.class,
        PaperChannelServiceImpl.class,
        PaperChannelUtils.class,
        PaperChannelResponseHandler.class,
        AnalogWorkflowPaperChannelResponseHandler.class,
        AuditLogServiceImpl.class,
        DigitalWorkFlowRetryHandler.class,
        CompletionWorkFlowHandler.class,
        NationalRegistriesResponseHandler.class,
        NationalRegistriesServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        NotificationProcessCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        NotificationViewedRequestHandler.class,
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
        PecDeliveryWorkflowLegalFactsGenerator.class,
        AnalogDeliveryFailureWorkflowLegalFactsGenerator.class,
        RefinementScheduler.class,
        RegisteredLetterSender.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        MVPParameterConsumer.class,
        NotificationCost.class,
        ViewNotification.class,
        PnDeliveryClientReactiveMock.class,
        PnDataVaultClientReactiveMock.class,
        DocumentCreationRequestServiceImpl.class,
        DocumentCreationRequestDaoMock.class,
        SafeStorageResponseHandler.class,
        DocumentCreationResponseHandler.class,
        ReceivedLegalFactCreationResponseHandler.class,
        ScheduleRecipientWorkflow.class,
        AarCreationResponseHandler.class,
        NotificationViewLegalFactCreationResponseHandler.class,
        DigitalDeliveryCreationResponseHandler.class,
        FailureWorkflowHandler.class,
        SuccessWorkflowHandler.class,
        NotificationValidationActionHandler.class,
        TaxIdPivaValidator.class,
        ReceivedLegalFactCreationRequest.class,
        NotificationValidationScheduler.class,
        DigitalWorkflowFirstSendRepeatHandler.class,
        SendAndUnscheduleNotification.class,
        AddressValidator.class,
        AddressManagerServiceImpl.class,
        AddressManagerClientMock.class,
        NormalizeAddressHandler.class,
        AddressManagerResponseHandler.class,
        DigitalTestWorkflowRepeatIT.SpringTestConfiguration.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@DirtiesContext
class DigitalTestWorkflowRepeatIT { 
    
    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private PaperChannelMock paperChannelMock;

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
    private NationalRegistriesClientMock nationalRegistriesClientMock;
    
    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;

    @Autowired
    private NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Autowired
    private ChooseDeliveryModeHandler chooseDeliveryType;
    
    @Autowired
    private StatusUtils statusUtils;
    
    @Autowired
    private PnExternalRegistryClient pnExternalRegistryClient;

    @Autowired
    private PaperChannelResponseHandler paperChannelResponseHandler;

    @Autowired
    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

    @Autowired
    private PaperChannelService paperChannelService;

    @Autowired
    private PaperChannelUtils paperChannelUtils;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private DocumentCreationRequestDaoMock documentCreationRequestDaoMock;

    @Autowired
    private PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock;

    @Autowired
    private AddressManagerClientMock addressManagerClientMock;
    
    @BeforeEach
    public void setup() {
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        TestUtils.initializeAllMockClient(
                safeStorageClientMock,
                pnDeliveryClientMock,
                addressBookMock,
                nationalRegistriesClientMock,
                timelineDaoMock,
                paperNotificationFailedDaoMock,
                pnDataVaultClientMock,
                pnDataVaultClientReactiveMock,
                documentCreationRequestDaoMock,
                addressManagerClientMock
        );
    }
    
    @Test
    void secondSuccessPlatform() {
        /*
       - Platform address presente e fallimento primo tentativo, il secondo ripetuto fallisce (Grazie all'inserimento di ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
            il terzo va a buon fine (valorizzando ExternalChannelMock.EXT_CHANNEL_WORKS is platformAddressSecondCycle)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente e fallimento primo tentativo (Ottenuto non valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */

        //NOTA: L'EXT_CHANNEL_SEND_FAIL_BOTH si riferisce sempre al secondo tentativo ripetuto
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo platform non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt platformAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("platformAddress2@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
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

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        addressBookMock.addSecondCycleLegalDigitalAddress(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddressSecondCycle));
       
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 5;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();
        
        checkExternalChannelAttemptSecondPlatformSuccess(platformAddress, digitalDomicile, pbDigitalAddress, platformAddressSecondCycle, iun, recIndex, notificationIntsEvents, digitalAddressesEvents);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddressSecondCycle, 1, 0);

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

    private void checkExternalChannelAttemptSecondPlatformSuccess(LegalDigitalAddressInt platformAddress,
                                                                  LegalDigitalAddressInt digitalDomicile,
                                                                  LegalDigitalAddressInt pbDigitalAddress,
                                                                  LegalDigitalAddressInt platformAddressSecondCycle,
                                                                  String iun,
                                                                  Integer recIndex,
                                                                  List<NotificationInt> notificationIntsEvents,
                                                                  List<LegalDigitalAddressInt> digitalAddressesEvents
    ) {
        int sentAttemptMade = 0;

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il terzo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade+= 1;

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address ripetuto del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il quinto tentativo sia avvenuto con il platform address aggiornato a valle del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddressSecondCycle.getAddress(), notificationIntsEvents.get(4).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddressSecondCycle.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.OK, false);
    }

    @Test
    void firstNotAvailableAndSecondSuccessPlatform() {
        /*
       - Platform non presente per il primo tentativo, il secondo ripetuto non avviene perchè non presente il primo
            il terzo va a buon fine (valorizzando ExternalChannelMock.EXT_CHANNEL_WORKS is platformAddressSecondCycle)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente e fallimento primo tentativo (Ottenuto non valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */
        
        //Il secondo tentativo platform non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt platformAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("platformAddress2@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
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

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addSecondCycleLegalDigitalAddress(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddressSecondCycle));

        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 3;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        checkExternalChannelAttemptFirstNotAvailableSecondPlatformSuccess(digitalDomicile, pbDigitalAddress, platformAddressSecondCycle, iun, recIndex, notificationIntsEvents, digitalAddressesEvents);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddressSecondCycle, 1, 0);

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


    private void checkExternalChannelAttemptFirstNotAvailableSecondPlatformSuccess(
                                                                  LegalDigitalAddressInt digitalDomicile,
                                                                  LegalDigitalAddressInt pbDigitalAddress,
                                                                  LegalDigitalAddressInt platformAddressSecondCycle,
                                                                  String iun,
                                                                  Integer recIndex,
                                                                  List<NotificationInt> notificationIntsEvents,
                                                                  List<LegalDigitalAddressInt> digitalAddressesEvents
    ) {
        int sentAttemptMade = 0;
        
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il terzo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade+= 1;
        
        //Viene verificato che il quinto tentativo sia avvenuto con il platform address aggiornato a valle del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddressSecondCycle.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddressSecondCycle.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.OK, false);
    }


    @Test
    void secondFailPlatform() {
        /*
       - Platform address presente e fallimento primo tentativo, il secondo ripetuto fallisce (Grazie all'inserimento di ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
            il terzo fallisce a sua volta (valorizzando ExternalChannelMock.EXT_CHANNEL_SEND_FAIL is platformAddressSecondCycle)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST) successo secondo tentativo
       - General address presente e fallimento primo tentativo (Ottenuto non valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */

        //NOTA: L'EXT_CHANNEL_SEND_FAIL_BOTH si riferisce sempre al secondo tentativo ripetuto
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo platform non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt platformAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("platformAddress2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        addressBookMock.addSecondCycleLegalDigitalAddress(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddressSecondCycle));

        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 6;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        checkExternalChannelAttemptSecondPlatformFail(platformAddress, digitalDomicile, pbDigitalAddress, platformAddressSecondCycle, iun, recIndex, notificationIntsEvents, digitalAddressesEvents);

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

    private void checkExternalChannelAttemptSecondPlatformFail(LegalDigitalAddressInt platformAddress,
                                                               LegalDigitalAddressInt digitalDomicile,
                                                               LegalDigitalAddressInt pbDigitalAddress,
                                                               LegalDigitalAddressInt platformAddressSecondCycle,
                                                               String iun,
                                                               Integer recIndex,
                                                               List<NotificationInt> notificationIntsEvents,
                                                               List<LegalDigitalAddressInt> digitalAddressesEvents
    ) {
        int sentAttemptMade = 0;

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il terzo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade+= 1;

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address ripetuto del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il quinto tentativo sia avvenuto con il platform address aggiornato a valle del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddressSecondCycle.getAddress(), notificationIntsEvents.get(4).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddressSecondCycle.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO, false);

        //Viene verificato che il sesto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(5).getIun(), digitalAddressesEvents.get(5).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.OK);

    }

    @Test
    void secondSuccessGeneral() {
        /*
       - Platform address presente e fallimento primo tentativo, il secondo ripetuto fallisce (Grazie all'inserimento di ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
            il terzo fallisce a sua volta (valorizzando ExternalChannelMock.EXT_CHANNEL_SEND_FAIL is platformAddressSecondCycle)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST) successo secondo tentativo
       - General address presente e fallimento primo tentativo e anche tentativo ripetuto, il terzo tenativo va a buon fine valorizzando pbDigitalAddressSecondCycle con EXT_CHANNEL_WORKS
    */

        //NOTA: L'EXT_CHANNEL_SEND_FAIL_BOTH si riferisce sempre al secondo tentativo ripetuto
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo platform non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt platformAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("platformAddress2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo GENERAL non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt pbDigitalAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress2@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        addressBookMock.addSecondCycleLegalDigitalAddress(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddressSecondCycle));
        
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);
        nationalRegistriesClientMock.addDigitalSecondCycle(recipient.getTaxId(), pbDigitalAddressSecondCycle);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 8;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        checkExternalChannelAttemptSecondGenera(platformAddress, digitalDomicile, pbDigitalAddress, platformAddressSecondCycle, pbDigitalAddressSecondCycle, iun, recIndex, notificationIntsEvents, digitalAddressesEvents, ResponseStatusInt.OK);

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddressSecondCycle, 1, 0);

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

    private void checkExternalChannelAttemptSecondGenera(LegalDigitalAddressInt platformAddress,
                                                         LegalDigitalAddressInt digitalDomicile,
                                                         LegalDigitalAddressInt pbDigitalAddress,
                                                         LegalDigitalAddressInt platformAddressSecondCycle,
                                                         LegalDigitalAddressInt pbDigitalAddressSecondCycle,
                                                         String iun,
                                                         Integer recIndex,
                                                         List<NotificationInt> notificationIntsEvents,
                                                         List<LegalDigitalAddressInt> digitalAddressesEvents,
                                                         ResponseStatusInt secondGeneralFeedback
    ) {
        int sentAttemptMade = 0;

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(0).getIun(), digitalAddressesEvents.get(0).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(1).getIun(), digitalAddressesEvents.get(1).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il terzo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(2).getIun(), digitalAddressesEvents.get(2).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        sentAttemptMade+= 1;

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address ripetuto del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddress.getAddress(), notificationIntsEvents.get(3).getIun(), digitalAddressesEvents.get(3).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO);

        //Viene verificato che il quinto tentativo sia avvenuto con il platform address aggiornato a valle del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, platformAddressSecondCycle.getAddress(), notificationIntsEvents.get(4).getIun(), digitalAddressesEvents.get(4).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(platformAddressSecondCycle.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.PLATFORM, ResponseStatusInt.KO, false);

        //Viene verificato che il sesto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, digitalDomicile.getAddress(), notificationIntsEvents.get(5).getIun(), digitalAddressesEvents.get(5).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(digitalDomicile.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.SPECIAL, ResponseStatusInt.KO);

        //Viene verificato che il settimo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici (si tratta di quello ripetuto)
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddress.getAddress(), notificationIntsEvents.get(6).getIun(), digitalAddressesEvents.get(6).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddress.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, ResponseStatusInt.KO);

        //Viene verificato che l'ottavo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici aggiornato a valle del primo tentativo
        TestUtils.checkExternalChannelPecSend(iun, pbDigitalAddressSecondCycle.getAddress(), notificationIntsEvents.get(7).getIun(), digitalAddressesEvents.get(7).getAddress());
        checkIsPresentAcceptanceAndFeedbackInTimeline(pbDigitalAddressSecondCycle.getAddress(), iun, recIndex, sentAttemptMade, DigitalAddressSourceInt.GENERAL, secondGeneralFeedback, false);
    }

    @Test
    void secondFailGeneral() {
        /*
       - Platform address presente e fallimento primo tentativo, il secondo ripetuto fallisce (Grazie all'inserimento di ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
            il terzo fallisce a sua volta (valorizzando ExternalChannelMock.EXT_CHANNEL_SEND_FAIL is platformAddressSecondCycle)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST) successo secondo tentativo
       - General address presente e fallimento primo tentativo e anche tentativo ripetuto, il terzo tenativo a sua volta fallisce pbDigitalAddressSecondCycle con EXT_CHANNEL_SEND_FAIL
    */

        //NOTA: L'EXT_CHANNEL_SEND_FAIL_BOTH si riferisce sempre al secondo tentativo ripetuto
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo platform non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt platformAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("platformAddress2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        //Il secondo tentativo GENERAL non ripetuto invece viene definito con un ulteriore indirizzo
        LegalDigitalAddressInt pbDigitalAddressSecondCycle = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId("ANON_"+taxId01)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        addressBookMock.addSecondCycleLegalDigitalAddress(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddressSecondCycle));

        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);
        nationalRegistriesClientMock.addDigitalSecondCycle(recipient.getTaxId(), pbDigitalAddressSecondCycle);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<NotificationInt> notificationIntEventCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> digitalAddressEventCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        int sentPecAttemptNumber = 8;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(notificationIntEventCaptor.capture(), Mockito.any(), digitalAddressEventCaptor.capture(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        List<NotificationInt> notificationIntsEvents = notificationIntEventCaptor.getAllValues();
        List<LegalDigitalAddressInt> digitalAddressesEvents = digitalAddressEventCaptor.getAllValues();

        checkExternalChannelAttemptSecondGenera(platformAddress, digitalDomicile, pbDigitalAddress, platformAddressSecondCycle, pbDigitalAddressSecondCycle, iun, recIndex, notificationIntsEvents, digitalAddressesEvents, ResponseStatusInt.KO);

        //Viene verificato che il workflow sia fallito
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


    private void checkIsPresentAcceptanceAndFeedbackInTimeline(String address,
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

    private void checkIsPresentAcceptanceAndFeedbackInTimeline(String address,
                                                               String iun,
                                                               Integer recIndex,
                                                               int sendAttemptMade,
                                                               DigitalAddressSourceInt platform,
                                                               ResponseStatusInt status,
                                                               Boolean isFirstRepeat) {
        LegalDigitalAddressInt digitalAddress = LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .address(address)
                .build();

        TestUtils.checkAcceptance(iun, recIndex, sendAttemptMade, digitalAddress, platform, timelineService, isFirstRepeat);
        TestUtils.checkDigitalFeedback(iun, recIndex, sendAttemptMade, digitalAddress, platform, timelineService, status, isFirstRepeat);
    }
}
