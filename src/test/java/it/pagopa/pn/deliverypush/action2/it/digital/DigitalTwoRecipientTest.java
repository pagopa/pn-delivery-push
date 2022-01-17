package it.pagopa.pn.deliverypush.action2.it.digital;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
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

import java.time.Instant;
import java.util.ArrayList;
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
        PnDeliveryPushConfigs.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        DigitalTwoRecipientTest.SpringTestConfiguration.class
})
class DigitalTwoRecipientTest {
    /* Primo recipient
       - Platform address presente e primo invio con fallimento
       - Special address presente e primo invio con successo
       - General address vuoto (Ottenuto non valorizzando
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente e primo invio con fallimento ma secondo con successo 
       - General address vuoto
    */

    //Primo Recipient
    private static final DigitalAddress platformAddress1 = DigitalAddress.builder()
            .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress digitalDomicile1 = DigitalAddress.builder()
            .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
            .type(DigitalAddressType.PEC)
            .build();

    private static final NotificationRecipient recipient1 = NotificationRecipientTestBuilder.builder()
            .withTaxId("TAXID01")
            .withDigitalDomicile(digitalDomicile1)
            .build();

    private static final AddressBookEntry addressBookEntry1 = AddressBookEntryTestBuilder.builder()
            .withTaxId(recipient1.getTaxId())
            .withPlatformAddress(platformAddress1)
            .build();

    //Secondo recipient
    private static final DigitalAddress platformAddress2 = DigitalAddress.builder()
            .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress digitalDomicile2 = DigitalAddress.builder()
            .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
            .type(DigitalAddressType.PEC)
            .build();

    private static final NotificationRecipient recipient2 = NotificationRecipientTestBuilder.builder()
            .withTaxId("TAXID02")
            .withDigitalDomicile(digitalDomicile2)
            .build();

    private static final AddressBookEntry addressBookEntry2 = AddressBookEntryTestBuilder.builder()
            .withTaxId(recipient2.getTaxId())
            .withPlatformAddress(platformAddress2)
            .build();


    private static final Map<String, DigitalAddress> PUB_REGISTRY_DIGITAL = Collections.emptyMap();
    private static final Map<String, PhysicalAddress> PUB_REGISTRY_PHYSICAL = Collections.emptyMap();

    private static final List<NotificationRecipient> recipients = new ArrayList<>();
    private static final List<AddressBookEntry> addressBookEntries = new ArrayList<>();

    private static Notification notification;

    public DigitalTwoRecipientTest() {
        recipients.add(recipient1);
        recipients.add(recipient2);
        addressBookEntries.add(addressBookEntry1);
        addressBookEntries.add(addressBookEntry2);

        notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipients(recipients)
                .build();
    }

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(notification, addressBookEntries, PUB_REGISTRY_DIGITAL, PUB_REGISTRY_PHYSICAL);
        }
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
    private CompletionWorkFlowHandler completionWorkflow;

    @BeforeEach
    public void setup() {
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
    }

    @Test
    void workflowTest() {
        String iun = notification.getIun();
        String taxIdRecipient1 = recipient1.getTaxId();
        String taxIdRecipient2 = recipient2.getTaxId();

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, taxIdRecipient1, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, taxIdRecipient1, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient1, sendPecEvent, 0, platformAddress1.getAddress());
        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient1, sendPecEvent, 1, digitalDomicile1.getAddress());

        //Viene verificato per il primo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, taxIdRecipient1, timelineService, completionWorkflow, digitalDomicile1, 2, 0);

        //Viene verificato per il primo recipient che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, taxIdRecipient1, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient2, sendPecEvent, 2, platformAddress2.getAddress());
        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient2, sendPecEvent, 3, digitalDomicile2.getAddress());
        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient2, sendPecEvent, 2, platformAddress2.getAddress());
        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, taxIdRecipient2, sendPecEvent, 3, digitalDomicile2.getAddress());

        //Viene verificato per il secondo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, taxIdRecipient2, timelineService, completionWorkflow, digitalDomicile2, 2, 1);

        //Viene verificato per il secondo recipient che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, taxIdRecipient2, timelineService);

    }
}
