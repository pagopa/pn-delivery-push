package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.PaperNotificationFailedDaoMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.TimelineDaoMock;
import it.pagopa.pn.deliverypush.action2.it.utils.AddressBookEntryTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
        ExtChnEventUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        AttachmentServiceImpl.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        ValidationDocumentError.SpringTestConfiguration.class
})
class ValidationDocumentError {
    /*
   - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
   - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
   - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
    */

    private static final DigitalAddress platformAddress = DigitalAddress.builder()
            .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress digitalDomicile = DigitalAddress.builder()
            .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress pbDigitalAddress = DigitalAddress.builder()
            .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
            .type(DigitalAddressType.PEC)
            .build();

    private static final NotificationRecipient recipient = NotificationRecipientTestBuilder.builder()
            .withTaxId("TAXID01")
            .withDigitalDomicile(digitalDomicile)
            .build();

    private static final Notification notification = NotificationTestBuilder.builder()
            .withIun("IUN01")
            .withNotificationRecipient(recipient)
            .build();

    private static final AddressBookEntry addressBookEntry = AddressBookEntryTestBuilder.builder()
            .withTaxId(recipient.getTaxId())
            .withPlatformAddress(platformAddress)
            .build();

    private static final Map<String, DigitalAddress> PUB_REGISTRY_DIGITAL = Collections.singletonMap(recipient.getTaxId(), pbDigitalAddress);
    private static final Map<String, PhysicalAddress> PUB_REGISTRY_PHYSICAL = Collections.emptyMap();

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(notification, addressBookEntry, PUB_REGISTRY_DIGITAL, PUB_REGISTRY_PHYSICAL);
        }
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
    private CompletionWorkFlowHandler completionWorkflow;
    
    @SpyBean
    private FileStorage fileStorage;

    @SpyBean
    private NotificationReceiverValidator notificationReceiverValidator;
    
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
        
        Set<ConstraintViolation<DigestEqualityBean>> errors = new HashSet<>();;
        doThrow(new PnValidationException("key", errors )).when(notificationReceiverValidator).checkPreloadedDigests(Mockito.any(),Mockito.any(),Mockito.any());

    }

    @Test
    void workflowTest() {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REQUEST_REFUSED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())).isPresent());
    }


}
