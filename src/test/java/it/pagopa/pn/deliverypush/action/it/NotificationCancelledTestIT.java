package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogDeliveryFailureWorkflowLegalFactsGenerator;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
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
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.*;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.*;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.RecipientType;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.responsehandler.*;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import it.pagopa.pn.deliverypush.utils.PaperSendModeUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        CompletionWorkFlowHandler.class,
        NationalRegistriesResponseHandler.class,
        NationalRegistriesServiceImpl.class,
        ExternalChannelServiceImpl.class,
        ExternalChannelResponseHandler.class,
        SafeStorageServiceImpl.class,
        RefinementHandler.class,
        NotificationViewedRequestHandler.class,
        IoServiceImpl.class,
        NotificationProcessCostServiceImpl.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        AarUtils.class,
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
        TimelineCounterDaoMock.class,
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
        NotificationCancellationServiceImpl.class,
        AuthUtils.class,
        MandateServiceImpl.class,
        NotificationCancellationActionHandler.class,
        NotificationCancelledTestIT.SpringTestConfiguration.class,
        F24Validator.class,
        F24ClientMock.class,
        PnExternalRegistriesClientReactiveMock.class,
        PaymentValidator.class,
        NotificationRefusedActionHandler.class,
        PaperSendModeUtils.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@DirtiesContext
class NotificationCancelledTestIT {
    
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

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;
    
    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @Autowired
    private SafeStorageClientMock safeStorageClientMock;
    
    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private PnDeliveryClientReactiveMock pnDeliveryClientReactiveMock;

    @Autowired
    private UserAttributesClientMock addressBookMock;

    @Autowired
    private NationalRegistriesClientMock nationalRegistriesClientMock;
    
    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private TimelineCounterDaoMock timelineCounterDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;
   
    @Autowired
    private NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Autowired
    private StatusUtils statusUtils;

    @SpyBean
    private SaveLegalFactsService legalFactStore;

    @SpyBean
    private PaperNotificationFailedService paperNotificationFailedService;

    @SpyBean
    private TimelineService timelineService;

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

    @MockBean
    private MandateService mandateService;

    @Autowired
    private NotificationCancellationService notificationCancellationService;

    @Autowired
    private PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock;

    @Autowired
    private DocumentCreationRequestDaoMock documentCreationRequestDaoMock;

    @Autowired
    private AddressManagerClientMock addressManagerClientMock;

    @Autowired
    private NotificationCancellationActionHandler notificationCancellationActionHandler;

    @Autowired
    private F24ClientMock f24ClientMock;

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
                addressManagerClientMock,
                f24ClientMock
        );
    }

    @Test
    void notificationCancelled() {

        String iun = TestUtils.getRandomIun();
        String taxId = TimelineDaoMock.SIMULATE_CANCEL_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .sentAttemptMade(0)
                .build()
        );

        NotificationInt notification = commonExecution(iun, taxId,1);

        commonChecks(notification, 0, TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build());
    }

    @Test
    void notificationCancelledAfterViewed() {
        // controlla che la notifica vada in annullata DOPO la visualizzazione
        String iun = TestUtils.getRandomIun();
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .sentAttemptMade(0)
                .build()
        ) + TimelineDaoMock.SIMULATE_AFTER_CANCEL_NOTIFICATION;

        NotificationInt notification = commonExecution(iun, taxId,1);

        commonChecks(notification, 0, TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build());
    }


    @Test
    void notificationCancelledIgnoreViewed() {
        // controlla che la notifica annullata resti tale anche DOPO la visualizzazione
        String iun = TestUtils.getRandomIun();
        String taxId = TimelineDaoMock.SIMULATE_CANCEL_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .sentAttemptMade(0)
                .build()
        );

        NotificationInt notification = commonExecution(iun, taxId, 1);

        //Simulazione visualizzazione della notifica per il secondo recipient
        Instant notificationViewDate2 = Instant.now();
        notificationViewedRequestHandler.handleViewNotificationDelivery(iun, 0, null, notificationViewDate2);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().atLeast(Duration.ofSeconds(1));

        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );


        commonChecks(notification, 0, TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build());


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
        Assertions.assertEquals(notificationViewDate, notificationViewTimelineElement.getTimestamp());
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
        Assertions.assertEquals(notificationViewDate, timelineElement.getTimestamp());

        NotificationViewedDetailsInt details = (NotificationViewedDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals(recIndex, details.getRecIndex());
        Assertions.assertEquals(delegateInfo, details.getDelegateInfo());
    }


    private void checkNotificationCancelledTimelineElement(String iun, int notrefined) {
        String timelineId = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineService.getTimelineElement(iun, timelineId );

        Assertions.assertTrue(timelineElementInternalOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternalOpt.get();
        Assertions.assertEquals(iun, timelineElement.getIun());

        NotificationCancelledDetailsInt details = (NotificationCancelledDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals(notrefined, details.getNotRefinedRecipientIndexes().size());
        Assertions.assertEquals(notrefined*100, details.getNotificationCost());

    }

    private void checkNoSendXXX(String iun, int recIndex) {


        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE );
        Optional<TimelineElementInternal> timelineElementInternalOpt1 = timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.SEND_ANALOG_DOMICILE );

        Assertions.assertTrue(timelineElementInternalOpt.isEmpty());
        Assertions.assertTrue(timelineElementInternalOpt1.isEmpty());
    }

    private NotificationInt commonExecution(String iun, String taxId, int notrefined){
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
                .withTaxId(taxId)
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
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
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

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        checkNotificationCancelledTimelineElement(iun, notrefined);
        for(int kk = 0;kk<notrefined;kk++)
            checkNoSendXXX(iun, kk);

        return notification;
    }


    private void commonChecks(NotificationInt notification, int recIndex, TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo ){

        //Viene effettuato il check dei legalFacts generati

        TestUtils.checkGeneratedLegalFacts(
                notification,
                notification.getRecipients().get(recIndex),
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
        TestUtils.writeAllGeneratedLegalFacts(notification.getIun(), className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void testNotificationCancelledTwoRecipient(){

        String iun = TestUtils.getRandomIun();

        //Primo Recipient
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxId01 = TimelineDaoMock.SIMULATE_CANCEL_NOTIFICATION +  TimelineEventId.AAR_GENERATION.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .sentAttemptMade(0)
                .build()
        );
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId01)
                .withInternalId(taxId01 +"anon")
                .withDigitalDomicile(digitalDomicile1)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        //Secondo recipient, solo cartaceo


        String taxId02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId02)
                .withInternalId(taxId02 + "ANON")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + "_Via Nuova1")
                                .build()
                )
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);


        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationRecipients(recipients)
                .withIun(iun)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));

        Integer recIndex1 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.CANCELLED, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        checkNotificationCancelledTimelineElement(iun, 2);

        commonChecks(notification, 0, TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build());

        commonChecks(notification, 1, TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build());
    }
}
