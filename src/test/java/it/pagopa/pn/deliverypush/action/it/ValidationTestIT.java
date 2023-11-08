package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
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
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.*;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.*;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.middleware.responsehandler.*;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static it.pagopa.pn.deliverypush.action.it.mockbean.F24ClientMock.F24_VALIDATION_FAIL;
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
        IoServiceImpl.class,
        NotificationProcessCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        StatusUtils.class,
        PublicRegistryUtils.class,
        NotificationUtils.class,
        PecDeliveryWorkflowLegalFactsGenerator.class,
        AnalogDeliveryFailureWorkflowLegalFactsGenerator.class,
        RefinementScheduler.class,
        RegisteredLetterSender.class,
        NotificationServiceImpl.class,
        StatusServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        TimeLineServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AddressBookServiceImpl.class,
        AttachmentUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        TimelineCounterDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        MVPParameterConsumer.class,
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
        NotificationCost.class,
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
        ValidationTestIT.SpringTestConfiguration.class,
        F24Validator.class,
        F24ClientMock.class,
        F24ResponseHandler.class,
        PnExternalRegistriesClientReactiveMock.class,
        PaymentValidator.class,
        NotificationRefusedActionHandler.class,
        PaperSendModeUtils.class
})
@TestPropertySource(
        locations ="classpath:/application-test.properties",
        properties = "pn.delivery-push.validation-document-test=true"
)
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@DirtiesContext
class ValidationTestIT {
    
    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private PaperChannelMock paperChannelMock;

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
    private TimelineCounterDaoMock timelineCounterDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;

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

    @Autowired
    private F24ClientMock f24ClientMock;

    @BeforeEach
    public void setup() {
        //Mock for get current date
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        ConsoleAppenderCustom.initializeLog();

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
    void differentShaRefusedTest() throws PnIdConflictException {
        // GIVEN
        
        // Platform address is present and all sending attempts fail
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //Special address is present and all sending attempts fail
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //General address is present and all sending attempts fail
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        byte[] differentFileSha = "error".getBytes();
        TestUtils.firstFileUploadFromNotificationError(notification, safeStorageClientMock, differentFileSha);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);
        
        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recIndex(recIndex)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void taxIdNotValidTest() throws PnIdConflictException {
        // GIVEN

        // Platform address is present and all sending attempts fail
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //Special address is present and all sending attempts fail
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        //General address is present and all sending attempts fail
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01_" + NationalRegistriesClientMock.NOT_VALID)
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        byte[] differentFileSha = "error".getBytes();
        TestUtils.firstFileUploadFromNotificationError(notification, safeStorageClientMock, differentFileSha);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        nationalRegistriesClientMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void addressNotValidTest() {
        // GIVEN
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_NOT_VALID_ADDRESS)
                        .build())
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
        
        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void fileTooBig() {
        // GIVEN

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                        .build())
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotificationTooBig(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }



    @Test
    void fileNotValidPDF() {
        // GIVEN

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(PhysicalAddressBuilder.builder()
                        .withAddress("Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                        .build())
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotificationNotAPDF(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }
    @Test
    void f24ValidationKo() {
        // GIVEN
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        String paymentDocName = "metadata_0_0";
        NotificationDocumentInt paymentDoc = TestUtils.getDocumentList(paymentDocName).get(0);
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withPhysicalAddress(paPhysicalAddress1)
                .withPayments(TestUtils.getPaymentWithF24(paymentDoc))
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        List<TestUtils.DocumentWithContent> listDocumentWithContentForPayments = TestUtils.getDocumentWithContents(paymentDocName, List.of(paymentDoc));

        //List which contains documents and payments
        List<TestUtils.DocumentWithContent> notificationDocuments = new ArrayList<>(listDocumentWithContent);
        notificationDocuments.addAll(listDocumentWithContentForPayments);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(TestUtils.getRandomIun() + F24_VALIDATION_FAIL)
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(notificationDocuments, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void validationPaymentInfoKO() {
        // GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId("creditorTaxId_"+PnExternalRegistriesClientReactiveMock.TO_FAIL+UUID.randomUUID())
                                        .noticeCode("noticeCode_"+UUID.randomUUID())
                                        .applyCost(true)
                                        .attachment(NotificationDocumentInt.builder()
                                                .ref(NotificationDocumentInt.Ref.builder()
                                                        .key("keyPagoPaForm")
                                                        .build())
                                                .digests(NotificationDocumentInt.Digests.builder()
                                                        .sha256(Base64Utils.encodeToString("keyPagoPaForm".getBytes()))
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                ))
                .build();


        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);

        //THEN
        await().atMost(Duration.ofSeconds(1000)).untilAsserted(() ->
                //Check worfklow is failed
                Assertions.assertTrue(timelineService.getTimelineElement(
                        iun,
                        TimelineEventId.REQUEST_REFUSED.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .build())).isPresent()
                )
        );

        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(LegalDigitalAddressInt.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        );
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        ConsoleAppenderCustom.checkLogs("Payment information is not valid");
    }

    @Test
    void validationPaymentInfoOK() {
        String iun = TestUtils.getRandomIun();

        String fileDocPayment = "keyPagoPaForm_doc00";
        List<NotificationDocumentInt> paymentDocuments = TestUtils.getDocumentList(fileDocPayment);
        List<TestUtils.DocumentWithContent> listPaymentDocumentWithContent = TestUtils.getDocumentWithContents(fileDocPayment, paymentDocuments);

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId("creditorTaxId_"+ UUID.randomUUID())
                                        .noticeCode("noticeCode_"+UUID.randomUUID())
                                        .applyCost(true)
                                        .attachment(paymentDocuments.get(0))
                                        .build())
                                .build()
                ))
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        TestUtils.firstFileUploadFromNotification(listPaymentDocumentWithContent, safeStorageClientMock);
        
        pnDeliveryClientMock.addNotification(notification);
        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        String timelineId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build()
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
    }

}
