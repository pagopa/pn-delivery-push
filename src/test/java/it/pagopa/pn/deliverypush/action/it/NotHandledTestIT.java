package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalFailureWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

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
@TestPropertySource( 
        locations ="classpath:/application-test.properties",
        properties = "pn.delivery-push.paper-message-not-handled=true"
)
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
class NotHandledTestIT {
    
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
    private CompletionWorkFlowHandler completionWorkflow;

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
    
    @BeforeEach
    public void setup() {
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        
        safeStorageClientMock.clear();
        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDeliveryClientMock.clear();
        pnDataVaultClientMock.clear();
    }

    @Test
    void digitalFailureWorkflowNotHandled() {

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

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(notification, safeStorageClientMock);

        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        pnDeliveryClientMock.addNotification(notification);
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in CANCELLED
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato il numero di send PEC verso external channel
        int sentPecAttemptNumber = 4;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(
                Mockito.eq(notification), Mockito.eq(recipient), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());

        //Viene verificato che la registered letter non sia stata inviata
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class), 
                Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());
        
        //Viene verificato che l'elemento di timeline relativo all'invio della registered letter non sia presente
        String eventIdRegisteredLetter = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<SimpleRegisteredLetterDetailsInt> sendSimpleRegisteredLetterOpt = timelineService.getTimelineElementDetails(iun, eventIdRegisteredLetter, SimpleRegisteredLetterDetailsInt.class);
        Assertions.assertFalse(sendSimpleRegisteredLetterOpt.isPresent());

        //Viene verificata la presenza dell'elemento di timeline NOT_HANDLED
        isPresentNotHandled(iun, recIndex);

        //Viene verificata la presenza dell'elemento di timeline di fallimento
        isPresentDigitalFailureWorkflow(notification, recIndex);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.checkPecDeliveryWorkflowLegalFactsGeneration(
                notification,
                timelineService,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    @Test
    void digitalFailureWorkflowNotHandledViewed() {
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

        String iun = "IUN01";
        //Viene simulata la visualizzazione della notifica
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .build()
        );
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(notification, safeStorageClientMock);

        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        pnDeliveryClientMock.addNotification(notification);
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che l'ultimo feedback sia arrivato
        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(0)
                        .index(1)
                        .source(DigitalAddressSourceInt.GENERAL)
                        .build()
        );
        
        //Prima di verificare la condizione vengono attesi due secondi dal momento che l'ultimo elemento di timeline viene inserito prima del verificarsi delle condizioni attese
        with().pollDelay(2, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(notification.getIun(), elementId).isPresent())
        );

        //Viene verificato il numero di send PEC verso external channel
        int sentPecAttemptNumber = 4;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(
                Mockito.eq(notification), Mockito.eq(recipient), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());

        //Viene verificato che la registered letter non sia stata inviata
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        //Viene verificato che l'elemento di timeline relativo all'invio della registered letter non sia presente
        String eventIdRegisteredLetter = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<SimpleRegisteredLetterDetailsInt> sendSimpleRegisteredLetterOpt = timelineService.getTimelineElementDetails(iun, eventIdRegisteredLetter, SimpleRegisteredLetterDetailsInt.class);
        Assertions.assertFalse(sendSimpleRegisteredLetterOpt.isPresent());

        //Dal momento che la notifica è stata visualizzata non dovrà essere presente l'elemento di timeline NOT HANDLED
        isNotPresentNotHandled(iun, recIndex);

        //Anche se la notifica è stata visualizzata non dovrà essere presente l'elemento di timeline di fallimento
        isPresentDigitalFailureWorkflow(notification, recIndex);

        //Viene effettuato il check dei legalFacts generati
        TestUtils.checkPecDeliveryWorkflowLegalFactsGeneration(
                notification,
                timelineService,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                EndWorkflowStatus.FAILURE,
                legalFactGenerator
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    @Test
    void sendAnalogNotHandled() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxId = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(notification, safeStorageClientMock);

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

        // Viene atteso fino a che lo stato non passi in CANCELLED
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato il numero di send PEC verso external channel
        int sentPecAttemptNumber = 6;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(
                Mockito.eq(notification), Mockito.eq(recipient), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);
        
        //Viene verificato che non ci sia stato nessun invio verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        checkNotSendAnalogNotification(iun, recIndex);

        //Viene verificata la presenza dell'elemento di timeline NOT_HANDLED
        isPresentNotHandled(iun, recIndex);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }

    @Test
    void sendAnalogNotHandledViewed() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = "IUN01";

        //Viene simulata la visualizzazione della notifica
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(notification, safeStorageClientMock);

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@works.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che non viene inserito lo schedule analog workflow
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(0)
                        .build()
        );

        //Prima di verificare la condizione vengono attesi due secondi dal momento che l'ultimo elemento di timeline viene inserito prima del verificarsi delle condizioni attese
        with().pollDelay(2, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(notification.getIun(), elementId).isPresent())
        );

        //Viene verificato il numero di send PEC verso external channel
        int sentPecAttemptNumber = 6;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(
                Mockito.eq(notification), Mockito.eq(recipient), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());

        //Viene verificato che non ci sia stato nessun invio verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        checkNotSendAnalogNotification(iun, recIndex);

        //Viene verificata la presenza dell'elemento di timeline NOT_HANDLED
        isNotPresentNotHandled(iun, recIndex);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);
    }
    
    private void isPresentNotHandled(String iun, Integer recIndex) {
        String eventIdNotHandled = TimelineEventId.NOT_HANDLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<NotHandledDetailsInt> notHandledDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdNotHandled, NotHandledDetailsInt.class);
        Assertions.assertTrue(notHandledDetailsOpt.isPresent());
        NotHandledDetailsInt notHandledDetails = notHandledDetailsOpt.get();

        Assertions.assertEquals(recIndex, notHandledDetails.getRecIndex());
        Assertions.assertEquals(NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE, notHandledDetails.getReasonCode());
        Assertions.assertEquals(NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON, notHandledDetails.getReason());
    }

    private void isNotPresentNotHandled(String iun, Integer recIndex) {
        String eventIdNotHandled = TimelineEventId.NOT_HANDLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<NotHandledDetailsInt> notHandledDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdNotHandled, NotHandledDetailsInt.class);
        Assertions.assertFalse(notHandledDetailsOpt.isPresent());
    }

    private void isPresentDigitalFailureWorkflow(NotificationInt notification, Integer recIndex) {
        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> digitalFailureWorkflowOpt = timelineService.getTimelineElement(notification.getIun(), elementId);
        Assertions.assertTrue(digitalFailureWorkflowOpt.isPresent());
        TimelineElementInternal digitalFailureWorkflow = digitalFailureWorkflowOpt.get();

        Assertions.assertNotNull(digitalFailureWorkflow.getLegalFactsIds());
        Assertions.assertNotNull(digitalFailureWorkflow.getLegalFactsIds().get(0));

        DigitalFailureWorkflowDetailsInt digitalFailureWorkflowDetails = (DigitalFailureWorkflowDetailsInt) digitalFailureWorkflow.getDetails();
        Assertions.assertEquals(recIndex, digitalFailureWorkflowDetails.getRecIndex());
    }
    
    private void checkNotSendAnalogNotification(String iun, Integer recIndex) {
        String eventIdSendAnalog = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(0)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdSendAnalog, SendAnalogDetailsInt.class);
        Assertions.assertFalse(sendPaperDetailsOpt.isPresent());
    }

}
