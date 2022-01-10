package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestUtils {
    public static final String EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT = "EXT_ANALOG_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_FAILURE = "INVESTIGATION_ADDRESS_PRESENT_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_POSITIVE = "INVESTIGATION_ADDRESS_PRESENT_POSITIVE";

    public static String PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS = "PB_DIGITAL_FAILURE";
    public static String PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS = "PB_ANALOG_FAILURE";
    public static String PUBLIC_REGISTRY_OK_GET_ANALOG_ADDRESS_WITH_FAILURE_ADDRESS = "PB_ANALOG_OK_FAILURE_ADDRESS";

    protected static Collection<Notification> getListNotification() {

        return Collections.singletonList(Notification.builder()
                .iun("IUN01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01_" + PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS + "_" + PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(PhysicalAddress.builder()
                                        .at("Presso")
                                        .address("Via di casa sua - " + EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT + "_" + INVESTIGATION_ADDRESS_PRESENT_FAILURE)
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
                .build());
    }

    protected static List<AddressBookEntry> getListAddressBook(String taxId) {
        return Collections.singletonList(AddressBookEntry.builder()
                .taxId(taxId)
                .courtesyAddresses(Collections.singletonList((DigitalAddress.builder()
                        .address("Via nuova 26")
                        .type(DigitalAddressType.PEC)
                        .build())))
                .build());
    }
}
