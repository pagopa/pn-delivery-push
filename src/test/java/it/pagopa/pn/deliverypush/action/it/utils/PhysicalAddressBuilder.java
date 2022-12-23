package it.pagopa.pn.deliverypush.action.it.utils;


import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public class PhysicalAddressBuilder {
    private String address;
    private String fullName;

    public static PhysicalAddressBuilder builder() {
        return new PhysicalAddressBuilder();
    }

    public PhysicalAddressBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public PhysicalAddressBuilder withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public PhysicalAddressInt build() {
        return PhysicalAddressInt.builder()
                .fullname(fullName)
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
