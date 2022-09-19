package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        PnAuditLogBuilder.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistryServiceImpl.class,
        ExternalChannelServiceImpl.class,
        ExternalChannelResponseHandler.class,
        SafeStorageServiceImpl.class,
        RefinementHandler.class,
        NotificationViewedHandler.class,
        IoServiceImpl.class,
        NotificationCostServiceImpl.class,
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
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        PnDeliveryPushConfigs.class,
        NotificationViewedTestIT.SpringTestConfiguration.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
class NotificationViewedTestIT {

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;
    
    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @SpyBean
    private ExternalChannelMock externalChannelMock;
    
    @SpyBean
    private PnSafeStorageClient safeStorageClient;
    
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
    private StatusUtils statusUtils;

    @SpyBean
    private SaveLegalFactsService legalFactStore;

    @SpyBean
    private PaperNotificationFailedService paperNotificationFailedService;

    @SpyBean
    private TimelineService timelineService;

    @BeforeEach
    public void setup() {
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        //File mock to return for getFileAndDownloadContent
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        fileCreationResponse.setKey("123");
        fileDownloadResponse.setChecksum("123");

        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");

        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn( fileDownloadResponse);
        
        Mockito.when( safeStorageClient.createFile(Mockito.any(FileCreationWithContentRequest.class), Mockito.anyString())).thenReturn(fileCreationResponse);
        Mockito.when( safeStorageClient.updateFileMetadata(Mockito.anyString(), Mockito.any())).thenReturn(operationResultCodeResponse);

        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDeliveryClientMock.clear();
        pnDataVaultClientMock.clear();
    }

    @Test
    void notificationViewed(){
        //GIVEN

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
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun, true);

        
        //Simulazione visualizzazione della notifica
        notificationViewedHandler.handleViewNotification(iun, recIndex);
        
        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkTimelineElementIsPresent(iun, recIndex);

        Mockito.verify(legalFactStore, Mockito.times(1)).saveNotificationViewedLegalFact(eq(notification), eq(recipient), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getTaxId(), iun);

        //Simulazione seconda visualizzazione della notifica
        notificationViewedHandler.handleViewNotification(iun, recIndex);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione non siano avvenuti, dunque che l numero d'invocazioni dei metodi sia rimasto lo stesso

        Mockito.verify(legalFactStore, Mockito.times(1)).saveNotificationViewedLegalFact(eq(notification),eq(recipient), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getTaxId(), iun);
    }

    private void checkTimelineElementIsPresent(String iun, Integer recIndex) {
        String timelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineService.getTimelineElement(iun, timelineId );

        Assertions.assertTrue(timelineElementInternalOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternalOpt.get();
        Assertions.assertEquals(iun, timelineElement.getIun());
        Assertions.assertEquals(recIndex, ((NotificationViewedDetailsInt) timelineElement.getDetails()).getRecIndex());
    }

    @Test
    void testNotificationViewedTwoRecipient(){
        //Primo Recipient
        LegalDigitalAddressInt platformAddress1 = LegalDigitalAddressInt.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile1 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile1)
                .build();

        //Secondo recipient
        LegalDigitalAddressInt platformAddress2 = LegalDigitalAddressInt.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        LegalDigitalAddressInt digitalDomicile2 = LegalDigitalAddressInt.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID02")
                .withDigitalDomicile(digitalDomicile2)
                .build();

        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipients(recipients)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndex(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun, true);

        //Simulazione visualizzazione della notifica per il primo recipient
        notificationViewedHandler.handleViewNotification(iun, recIndex1);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkTimelineElementIsPresent(iun, recIndex1);

        Mockito.verify(legalFactStore, Mockito.times(1)).saveNotificationViewedLegalFact(eq(notification),eq(recipient1), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient1.getTaxId(), iun);

        //Simulazione visualizzazione della notifica per il primo recipient
        notificationViewedHandler.handleViewNotification(iun, recIndex2);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti
        checkTimelineElementIsPresent(iun, recIndex2);

        Mockito.verify(legalFactStore, Mockito.times(1)).saveNotificationViewedLegalFact(eq(notification),eq(recipient2), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient1.getTaxId(), iun);
    }

    @Test
    void notificationViewedAfterRefinement(){
        //GIVEN

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
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun, true);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Simulazione visualizzazione della notifica
        notificationViewedHandler.handleViewNotification(iun, recIndex);

        //Viene effettuata la verifica che i processi correlati alla visualizzazione siano avvenuti e siano corretti
        String timelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> timelineElementViewedOpt = timelineService.getTimelineElement(iun, timelineId );

        Assertions.assertTrue(timelineElementViewedOpt.isPresent());
        TimelineElementInternal timelineElementViewed = timelineElementViewedOpt.get();
        Assertions.assertEquals(iun, timelineElementViewed.getIun());
        NotificationViewedDetailsInt details = (NotificationViewedDetailsInt) timelineElementViewed.getDetails();
        Assertions.assertEquals(recIndex, details.getRecIndex());
        Assertions.assertNull(details.getNotificationCost()); //Il costo deve essere null perchè la visualizzazione è avvenuta a valle del Refinement
        
        Mockito.verify(legalFactStore, Mockito.times(1)).saveNotificationViewedLegalFact(eq(notification), eq(recipient), Mockito.any(Instant.class));
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).deleteNotificationFailed(recipient.getTaxId(), iun);

        //Viene effettuata la verifica che il perfezionamento per decorrenza termini sia avvenuto e sia valorizzato correttamente
        Optional<TimelineElementInternal> timelineElementRefinementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));

        Assertions.assertTrue(timelineElementRefinementOpt.isPresent());
        TimelineElementInternal timelineElementRefinement = timelineElementRefinementOpt.get();
        RefinementDetailsInt detailsInt = (RefinementDetailsInt) timelineElementRefinement.getDetails();
        Assertions.assertNotNull(detailsInt.getNotificationCost()); //Il costo della notifica deve essere presente su Refinement perchè avvenuto in precedenza rispetto alla presa visione
    }
}
