package it.pagopa.pn.deliverypush.action2.it.digital;

import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.AbstractWorkflowTestConfiguration;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.PaperNotificationFailedDaoMock;
import it.pagopa.pn.deliverypush.action2.it.mockbean.TimelineDaoMock;
import it.pagopa.pn.deliverypush.action2.it.utils.AddressBookEntryTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        ExternalChannelResponseHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistrySendHandler.class,
        ExternalChannelSendHandler.class,
        RefinementHandler.class,
        LegalfactsMetadataUtils.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        ExtChnEventUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        ChooseDeliveryModeUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        DigitalEmptyFirstSuccessSpecialTest.SpringTestConfiguration.class
})
class DigitalEmptyFirstSuccessSpecialTest {
    /*
       - Platform address vuoto (Ottenuto non valorizzando nessun platformAddress in addressBookEntry)
       - Special address presente e primo invio con successo (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */


    private static final DigitalAddress digitalDomicile = DigitalAddress.builder()
            .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
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
            .build();

    private static final Map<String, DigitalAddress> PUB_REGISTRY_DIGITAL = Collections.emptyMap();
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

    @BeforeEach
    public void setup() {
        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplate("test");
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webapp);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
    }

    @Test
    void workflowTest() {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, taxId, false, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, taxId, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(1)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, taxId, sendPecEvent, 0, digitalDomicile.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, taxId, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, taxId, timelineService);
    }
}
