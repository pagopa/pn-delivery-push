package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.it.utils.AddressBookEntryTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.externalclient.addressbook.AddressBookEntry;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.datavault.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import it.pagopa.pn.deliverypush.validator.preloaded_digest_error.DigestEqualityBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintViolation;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.doThrow;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistrySendHandler.class,
        ExternalChannelSendHandler.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        LegalfactsMetadataUtils.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        StatusUtils.class,
        PublicRegistryUtils.class,
        NotificationUtils.class,
        NotificationServiceImpl.class,
        StatusServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        TimeLineServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        CheckAttachmentUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        ValidationDocumentErrorTestIT.SpringTestConfiguration.class
})
class ValidationDocumentErrorTestIT {
    
    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    @SpyBean
    private ExternalChannelMock externalChannelMock;
    
    @SpyBean
    private PnSafeStorageClient safeStorageClient;

    @SpyBean
    private NotificationReceiverValidator notificationReceiverValidator;

    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private AddressBookMock addressBookMock;

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

    @BeforeEach
    public void setup() {
        //Waiting time for action
        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //Direct access url, not useful for this test
        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplate("test");
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webapp);
        
        //Mock for get current date
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

        Mockito.when( safeStorageClient.getFile( Mockito.anyString(), Mockito.eq(false)))
                .thenReturn( fileDownloadResponse );
        
        //Clear mock
        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
    }

    @Test
    void workflowTest() throws IdConflictException {
        
        // GIVEN
        
        // Platform address is present and all sending attempts fail
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();
        
        //Special address is present and all sending attempts fail
        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();
        
        //General address is present and all sending attempts fail
        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient(recipient)
                .build();

        AddressBookEntry addressBookEntry = AddressBookEntryTestBuilder.builder()
                .withTaxId(recipient.getTaxId())
                .withPlatformAddress(platformAddress)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.add(addressBookEntry);
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        Set<ConstraintViolation<DigestEqualityBean>> errors = new HashSet<>();
        doThrow(new PnValidationException("key", errors )).when(notificationReceiverValidator).checkPreloadedDigests(Mockito.any(),Mockito.any(),Mockito.any());

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);
        
        //THEN
        
        //Check worfklow is failed
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REQUEST_REFUSED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
        
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendNotification(Mockito.any(PnExtChnEmailEvent.class));
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendNotification(Mockito.any(PnExtChnPecEvent.class));
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendNotification(Mockito.any(PnExtChnPaperEvent.class));
    }


}
