package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

public class TestUtils {
    public static final String EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT = "EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT";
    public static final String INVESTIGATION_ADDRESS_PRESENT_FAILURE = "INVESTIGATION_ADDRESS_PRESENT_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_POSITIVE = "INVESTIGATION_ADDRESS_PRESENT_POSITIVE";

    public static final String PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS";
    public static final String PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS";

    public static final PhysicalAddress PHYSICAL_ADDRESS_FAILURE_BOTH = PhysicalAddress.builder()
            .at("Presso")
            .address("Via nuova 14 - " + EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT)
            .zip("00100")
            .municipality("Roma")
            .province("RM")
            .foreignState("IT")
            .addressDetails("Scala A")
            .build();

    public static final PhysicalAddress PHYSICAL_ADDRESS_OK = PhysicalAddress.builder()
            .at("Presso")
            .address("Via nuova 14")
            .zip("00100")
            .municipality("Roma")
            .province("RM")
            .foreignState("IT")
            .addressDetails("Scala A")
            .build();

    public static PhysicalAddress getPhysicalAddressWithTaxIdForPublicRegistry(String taxId) {
        return PhysicalAddress.builder()
                .at("Presso")
                .address("Via nuova 14 - " + taxId)
                .zip("00100")
                .municipality("Roma")
                .province("RM")
                .foreignState("IT")
                .addressDetails("Scala A")
                .build();
    }
}