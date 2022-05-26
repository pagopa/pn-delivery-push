package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
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
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.externalclient.addressbook.AddressBookEntry;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.util.StatusUtils;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

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
        NotificationViewedHandler.class,
        LegalfactsMetadataUtils.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
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
        ConfidentialInformationServiceImpl.class,
        CheckAttachmentUtils.class,
        NotificationUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        NotificationViewedTestIT.SpringTestConfiguration.class
})
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
    
    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    @SpyBean
    private ExternalChannelMock externalChannelMock;
    
    @SpyBean
    private FileStorage fileStorage;
    
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
   
    @Autowired
    private NotificationViewedHandler notificationViewedHandler;

    @SpyBean
    private LegalFactDao legalFactStore;

    @SpyBean
    private PaperNotificationFailedService paperNotificationFailedService;

    @SpyBean
    private TimelineService timelineService;

    @BeforeEach
    public void setup() {
        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplate("test");
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webapp);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        FileData fileData = FileData.builder()
                .content( new ByteArrayInputStream("Body".getBytes(StandardCharsets.UTF_8)) )
                .build();

        // Given
        Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
                .thenReturn( fileData );

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

        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

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

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        //Viene completato un workflow di notifica di una notifica 
        
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
        Assertions.assertEquals(recIndex, timelineElement.getDetails().getRecIndex());
    }

    @Test
    void testNotificationViewedTwoRecipient(){
        //Primo Recipient
        DigitalAddress platformAddress1 = DigitalAddress.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile1 = DigitalAddress.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile1)
                .build();

        AddressBookEntry addressBookEntry1 = AddressBookEntryTestBuilder.builder()
                .withTaxId(recipient1.getTaxId())
                .withPlatformAddress(platformAddress1)
                .build();

        //Secondo recipient
        DigitalAddress platformAddress2 = DigitalAddress.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile2 = DigitalAddress.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID02")
                .withDigitalDomicile(digitalDomicile2)
                .build();

        AddressBookEntry addressBookEntry2 = AddressBookEntryTestBuilder.builder()
                .withTaxId(recipient2.getTaxId())
                .withPlatformAddress(platformAddress2)
                .build();


        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipients(recipients)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.add(addressBookEntry1);
        addressBookMock.add(addressBookEntry2);

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndex(notification, recipient2.getTaxId());


        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

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
}
