package it.pagopa.pn.deliverypush.action2.it.utils;



public class PhysicalAddressBuilder {
    String address;

    public static PhysicalAddressBuilder builder() {
        return new PhysicalAddressBuilder();
    }

    public PhysicalAddressBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public PhysicalAddress build() {
        return PhysicalAddress.builder()
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
