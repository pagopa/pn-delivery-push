package it.pagopa.pn.deliverypush.action.it;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.analogworkflow.*;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtilsImpl;
import it.pagopa.pn.deliverypush.action.completionworkflow.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
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
import it.pagopa.pn.deliverypush.config.SendMoreThan20GramsParameterConsumer;
import it.pagopa.pn.deliverypush.legalfacts.AarTemplateStrategyFactory;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.legalfacts.DynamicRADDExperimentationChooseStrategy;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ActionHandler;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.middleware.responsehandler.*;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineMapperFactory;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.awaitility.Awaitility.setDefaultTimeout;

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
        ChooseDeliveryModeUtilsImpl.class,
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
        TimelineCounterDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        MVPParameterConsumer.class,
        NotificationCost.class,
        ViewNotification.class,
        PnDeliveryClientReactiveMock.class,
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
        CommonTestConfiguration.SpringTestConfiguration.class,
        F24Validator.class,
        F24ClientMock.class,
        PnExternalRegistriesClientReactiveMock.class,
        PaymentValidator.class,
        NotificationRefusedActionHandler.class,
        F24ResponseHandler.class,
        PaperChannelMock.class,
        NotificationPaidHandler.class,
        NotificationCancellationServiceImpl.class,
        AuthUtils.class,
        MandateServiceImpl.class,
        MandateClientMock.class,
        NotificationCancellationActionHandler.class,
        PnSendModeUtils.class,
        CheckAttachmentRetentionHandler.class,
        ActionPoolMock.class,
        SendDigitalFinalStatusResponseHandler.class,
        //quickWorkAroundForPN-9116
        SendMoreThan20GramsParameterConsumer.class,
        AarTemplateStrategyFactory.class,
        DynamicRADDExperimentationChooseStrategy.class,
        CheckRADDExperimentation.class,
        FeatureEnabledUtils.class,
        AnalogFinalStatusResponseHandler.class,
        ActionHandler.class,
        SmartMapper.class,
        TimelineMapperFactory.class,
        PnEmdIntegrationClientMock.class,
        DocumentComposition.class,
        PnEmdIntegrationClientMock.class,
        RefusalCostCalculator.class,
        PnTechnicalRefusalCostMode.class,
        LookupAddressHandler.class
})
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-testIT.properties")
@DirtiesContext
@EnableScheduling
public class CommonTestConfiguration {
    private static final String[] PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST = {"radd-expeAAArimentation-zip-1", "radd-experimentation-zip-2", "radd-experimentation-zip-3", "radd-experimentation-zip-4", "radd-experimentation-zip-5"};

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }
    @Autowired
    ActionPoolMock actionPoolMock;
    @Autowired
    SafeStorageClientMock safeStorageClientMock;
    @Autowired
    PnDeliveryClientMock pnDeliveryClientMock;
    @Autowired
    UserAttributesClientMock addressBookMock;
    @Autowired
    NationalRegistriesClientMock nationalRegistriesClientMock;
    @Autowired
    InstantNowSupplier instantNowSupplier;
    @Autowired
    TimelineDaoMock timelineDaoMock;
    @Autowired
    PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;
    @Autowired
    PnDataVaultClientMock pnDataVaultClientMock;
    @Autowired
    PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock;
    @Autowired
    DocumentCreationRequestDaoMock documentCreationRequestDaoMock;
    @Autowired
    AddressManagerClientMock addressManagerClientMock;
    @Autowired
    PnDeliveryPushConfigs cfg;
    
    @BeforeEach
    public void setup() {
        setDefaultTimeout(Duration.ofSeconds(120));

        // Viene creato un oggetto Answer per ottenere l'istante corrente al momento della chiamata ...
        Answer<Instant> answer = invocation -> Instant.now();
        // e configurato Mockito per restituire l'istante corrente al momento della chiamata
        Mockito.when(instantNowSupplier.get()).thenAnswer(answer);
        
        setcCommonsConfigurationPropertiesForTest(cfg);

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
                actionPoolMock
        );
    }

    private void setcCommonsConfigurationPropertiesForTest(PnDeliveryPushConfigs cfg) {
        // Impostazione delle proprietà TimeParams
        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        times.setNotificationNonVisibilityTime("21:00");
        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofSeconds(1));
        times.setAttachmentRetentionTimeAfterValidation(Duration.ofSeconds(5));
        times.setCheckAttachmentTimeBeforeExpiration(Duration.ofSeconds(2));
        times.setAttachmentTimeToAddAfterExpiration(Duration.ofSeconds(50));
        
        Mockito.when(cfg.getTimeParams()).thenReturn(times);

        // Impostazione delle proprietà PaperChannel
        PnDeliveryPushConfigs.PaperChannel paperChannel = new PnDeliveryPushConfigs.PaperChannel();
        PnDeliveryPushConfigs.SenderAddress senderAddress = new PnDeliveryPushConfigs.SenderAddress();
        senderAddress.setFullname("PagoPA S.p.A.");
        senderAddress.setAddress("Via Sardegna n. 38");
        senderAddress.setZipcode("00187");
        senderAddress.setCity("Roma");
        senderAddress.setPr("Roma");
        senderAddress.setCountry("Italia");
        paperChannel.setSenderAddress(senderAddress);
        Mockito.when(cfg.getPaperChannel()).thenReturn(paperChannel);

        // Impostazione delle proprietà Webapp
        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplatePhysical("http://localhost:8090/dist/direct_access_pf");
        webapp.setDirectAccessUrlTemplateLegal("http://localhost:8090/dist/direct_access_pg");
        webapp.setFaqUrlTemplateSuffix("faq.html");
        webapp.setQuickAccessUrlAarDetailSuffix("notifica?aar");
        webapp.setLandingUrl("https://www.dev.pn.pagopa.it");
        webapp.setRaddPhoneNumber("06.4520.2323");
        webapp.setAarSenderLogoUrlTemplate("TO_BASE64_RESOLVER:https://example.com/<PA_ID>/logo.png");
        Mockito.when(cfg.getWebapp()).thenReturn(webapp);
        
        // Impostazione delle proprietà ExternalChannel
        PnDeliveryPushConfigs.ExternalChannel externalChannel = new PnDeliveryPushConfigs.ExternalChannel();
        externalChannel.setDigitalCodesProgress(Collections.singletonList("C001"));
        externalChannel.setDigitalCodesRetryable(Arrays.asList("C008", "C010"));
        externalChannel.setDigitalCodesSuccess(Collections.singletonList("C003"));
        externalChannel.setDigitalCodesFail(Arrays.asList("C002", "C004", "C006", "C009"));
        externalChannel.setDigitalCodesFatallog(Arrays.asList("C008", "C010"));
        externalChannel.setDigitalRetryCount(-1);
        externalChannel.setDigitalRetryDelay(Duration.ofMinutes(10));
        externalChannel.setDigitalSendNoresponseTimeout(Duration.ofSeconds(50));
        Mockito.when(cfg.getExternalChannel()).thenReturn(externalChannel);

        // Impostazione delle proprietà di retention degli allegati
        Mockito.when(cfg.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);

        // Impostazione delle proprietà di validazione PDF
        Mockito.when(cfg.isCheckPdfValidEnabled()).thenReturn(true);
        Mockito.when(cfg.getCheckPdfSize()).thenReturn(DataSize.ofMegabytes(200));

        // Impostazione delle proprietà di PnSendMode
        List<String> pnSendModeList = new ArrayList<>();
        pnSendModeList.add("1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION");
        pnSendModeList.add("2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD");

        //Impostazione delle proprietà di shedLock
        Mockito.when(cfg.getTimelineLockDuration()).thenReturn(Duration.ofSeconds(60));

        Mockito.when(cfg.getPnSendMode()).thenReturn(pnSendModeList);

        //quickWorkAroundForPN-9116
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue()).thenReturn(true);
        
        //Set send fee
        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(100);

        Mockito.when(cfg.getErrorCorrectionLevelQrCode()).thenReturn(ErrorCorrectionLevel.H);

        List<String> pnRaddExperimentationStore = new ArrayList<>();
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[3]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[4]);
        Mockito.when(cfg.getRaddExperimentationStoresName()).thenReturn(pnRaddExperimentationStore);

        Mockito.when(cfg.getFeatureUnreachableRefinementPostAARStartDate()).thenReturn(Instant.parse("2024-11-27T00:00:00Z"));

        Mockito.when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        Mockito.when(cfg.getPfNewWorkflowStart()).thenReturn("2099-02-13T23:00:00Z");


        Mockito.when(cfg.getStartWriteBusinessTimestamp()).thenReturn(Instant.parse("2024-11-27T00:00:00Z"));
        Mockito.when(cfg.getStopWriteBusinessTimestamp()).thenReturn(Instant.parse("2099-11-27T00:00:00Z"));
        Mockito.when(cfg.getTemplateURLforPEC()).thenReturn("/templates-engine-private/v1/templates/notification-aar-for-pec");
        Mockito.when(cfg.getTemplatesEngineBaseUrl()).thenReturn("http://localhost:8090");
    }

}
