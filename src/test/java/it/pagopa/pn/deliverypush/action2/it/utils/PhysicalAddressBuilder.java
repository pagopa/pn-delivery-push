package it.pagopa.pn.deliverypush.action2.it.utils;


import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public class PhysicalAddressBuilder {
    String address;

    public static PhysicalAddressBuilder builder() {
        return new PhysicalAddressBuilder();
    }

    public PhysicalAddressBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public PhysicalAddressInt build() {
        return PhysicalAddressInt.builder()
                .at("Presso")
                .address(address)
                .zip("00100")
                .municipality("Roma")
                .province("RM")
                .foreignState("IT")
                .addressDetails("Scala A")
                .build();
    }
}
