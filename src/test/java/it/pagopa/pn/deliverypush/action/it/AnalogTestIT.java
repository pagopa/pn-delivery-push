package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
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
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ScheduleRecipientWorkflow;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationScheduler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.TaxIdValidation;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.*;
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
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactiveImpl;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.middleware.responsehandler.*;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        DigitalWorkFlowExternalChannelResponseHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        ExternalChannelResponseHandler.class,
        PaperChannelServiceImpl.class,
        PaperChannelUtils.class,
        PaperChannelResponseHandler.class,
        AnalogWorkflowPaperChannelResponseHandler.class,
        NationalRegistriesServiceImpl.class,
        AuditLogServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        NotificationCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        RefinementHandler.class,
        NotificationViewedRequestHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        ChooseDeliveryModeUtils.class,
        NotificationUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        StatusServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AddressBookServiceImpl.class,
        AttachmentUtils.class,
        StatusUtils.class,
        PecDeliveryWorkflowLegalFactsGenerator.class,
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
        PnDeliveryClientReactiveImpl.class,
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
        TaxIdValidation.class,
        ReceivedLegalFactCreationRequest.class,
        NotificationValidationScheduler.class,
        AnalogTestIT.SpringTestConfiguration.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
class AnalogTestIT {
    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    @SpyBean
    private LegalFactGenerator legalFactGenerator;

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private PaperChannelMock paperChannelMock;

    @SpyBean
    private CompletionWorkFlowHandler completionWorkflow;

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private PaperChannelResponseHandler paperChannelResponseHandler;

    @Autowired
    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

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
    private NotificationUtils notificationUtils;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;
    
    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;

    @Autowired
    private PaperChannelService paperChannelService;

    @Autowired
    private PaperChannelUtils paperChannelUtils;

    @Autowired
    private StatusUtils statusUtils;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private DocumentCreationRequestDaoMock documentCreationRequestDaoMock;

    @BeforeEach
    public void setup() {
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        safeStorageClientMock.clear();
        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        nationalRegistriesClientMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
        documentCreationRequestDaoMock.clear();
    }
    
    @Test
    @Disabled("Test fail only in build fase PN-3853")
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

        String iun = "IUN01";

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
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

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
                .build();
        
        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    @Disabled("Test fail only in build fase PN-3853")
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

        String iun = "IUN01";

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
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        String timelineId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        
        //Dal momento che l'ultimo elemento di timeline non viene inserito in prossimità della fine del workflow viene utilizzato un delay
        with().pollDelay(5, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
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
                TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
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
                .withIun("IUN01")
                .withPaId("paId01")
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
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);
        
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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.emptyList());

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificato che non sia stato inviato alcun messaggio di cortesia
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da pa
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress1, 0, timelineService);

        /*
        Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dall'investigazione
            checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_OK, 1);
        */

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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void completelyUnreachableTwoRecipient() {
 /*     PRIMO RECIPIENT
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che per i due recipient non si vada in refinement
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REFINEMENT.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex1)
                                        .build())).isPresent())
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
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient2
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex2, listCourtesyAddressRecipient2, timelineService, externalChannelMock);

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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);
        
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che per entrambi i recipient non sia stato raggiunto il refinement
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REFINEMENT.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex1)
                                        .build())).isPresent())
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

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                recIndex1,
                1,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );

        //Viene effettuato il check dei legalFacts generati per il secondo recipient
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                recIndex2,
                0,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
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
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer rec1Index = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer rec2Index = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REFINEMENT.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(rec1Index)
                                        .build())).isPresent())

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
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);

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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                rec1Index,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
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

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                rec2Index,
                1,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
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
        
        String iun = "IUN01";

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
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        Integer rec1Index = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer rec2Index = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);

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
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient1,
                rec1Index,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
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

        TestUtils.checkGeneratedLegalFacts(
                notification,
                recipient2,
                rec2Index,
                1,
                generatedLegalFactsInfo2,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService
        );
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

}
