package it.pagopa.pn.deliverypush.action2.it.digital;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.AbstractWorkflowTestConfiguration;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.NotificationServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

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
        SchedulerServiceMock.class,
        PublicRegistryMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        DigitalWorkflowTestOne.SpringTestConfiguration.class
})
class DigitalWorkflowTestOne {
    private static final List<Notification> listNotification = new ArrayList<>(getListNotification());
    private static final String taxId = listNotification.get(0).getRecipients().get(0).getTaxId();
    private static final List<AddressBookEntry> addressBookEntries = getListAddressBook(taxId);

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {

        public SpringTestConfiguration() {
            super(listNotification, addressBookEntries);
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @Test
    void workflowTest() {
        /*Workflow digitale
           - Platform address presente (Ottenuto valorizzando il digitalAddresses.getPlatform() dei digitaladdresses dell'address book Definito in LIST_ADDRESS_BOOK)
           - Special address vuoto (Ottenuto non valorizzando recipient.getDigitalDomicile() della notifica)
           - General address vuoto (Ottenuto inserendo testUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS nel taxId)
        */

        //Notifica utilizzata
        Notification notification = listNotification.get(0);
        String iun = notification.getIun();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());


    }

    public static Collection<Notification> getListNotification() {
        Notification notification = Notification.builder()
                .iun("IUN01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01_" + TestUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS + "_" + TestUtils.PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(PhysicalAddress.builder()
                                        .at("Presso")
                                        .address("Via di casa sua - " + TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT + "_" + TestUtils.INVESTIGATION_ADDRESS_PRESENT_FAILURE)
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .addressDetails("Scala A")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .ref(NotificationAttachment.Ref.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
        return Collections.singletonList(notification);
    }

    public static List<AddressBookEntry> getListAddressBook(String taxId) {
        AddressBookEntry addressBookEntry = AddressBookEntry.builder()
                .taxId(taxId)
                .courtesyAddresses(Collections.singletonList((DigitalAddress.builder()
                        .address("testEmail@test.it")
                        .type(DigitalAddressType.PEC)
                        .build())))
                .digitalAddresses(
                        DigitalAddresses.builder()
                                .platform(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("testPlatform@test.it")
                                        .build())
                                .build()
                ).build();
        return Collections.singletonList(addressBookEntry);
    }
}
