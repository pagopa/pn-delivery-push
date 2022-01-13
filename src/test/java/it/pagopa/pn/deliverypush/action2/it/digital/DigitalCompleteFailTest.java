package it.pagopa.pn.deliverypush.action2.it.digital;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
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
import org.junit.jupiter.api.Assertions;
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
        PublicRegistryResponseHandler.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        ExtChnEventUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PnDeliveryPushConfigs.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        DigitalCompleteFailTest.SpringTestConfiguration.class
})
class DigitalCompleteFailTest {
    /*
   - Platform address presente e invio fallito (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXTCHANNEL_SEND_FAIL)
   - Special address presente e invio fallito (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXTCHANNEL_SEND_FAIL)
   - General address presente e invio fallito (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXTCHANNEL_SEND_FAIL)
    */

    private static final DigitalAddress platformAddress = DigitalAddress.builder()
            .address(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " platformAddress@mail.com")
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress digitalDomicile = DigitalAddress.builder()
            .address(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " digitalDomicile@mail.com")
            .type(DigitalAddressType.PEC)
            .build();

    private static final DigitalAddress pbDigitalAddress = DigitalAddress.builder()
            .address(ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " pbDigitalAddress@mail.com")
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

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @BeforeEach
    public void setup() {
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
    }

    @Test
    void workflowTest() {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificato che l'indirizzo Platform risulti presente
        TestUtils.checkGetAddress(iun, taxId, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeHandler.START_SENT_ATTEMPT_NUMBER, timelineService);

        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);

        Mockito.verify(externalChannelMock, Mockito.times(6)).sendNotification(pnExtChnPecEventCaptor.capture());


        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 1, digitalDomicile.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 2, pbDigitalAddress.getAddress());

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 3, platformAddress.getAddress());
        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 4, digitalDomicile.getAddress());
        //Viene verificato che il sesto tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        checkExternalChannelPecSend(iun, taxId, sendPecEvent, 5, pbDigitalAddress.getAddress());

    }

    private void checkExternalChannelPecSend(String iun, String taxId, List<PnExtChnPecEvent> sendPecEvent, int sendTime, String address) {
        Assertions.assertEquals(iun, sendPecEvent.get(sendTime).getPayload().getIun());
        Assertions.assertEquals(taxId, sendPecEvent.get(sendTime).getPayload().getRecipientTaxId());
        Assertions.assertEquals(address, sendPecEvent.get(sendTime).getPayload().getPecAddress());
    }

}
