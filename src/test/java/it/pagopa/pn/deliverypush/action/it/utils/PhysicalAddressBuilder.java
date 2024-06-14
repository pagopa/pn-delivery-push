package it.pagopa.pn.deliverypush.action.it.utils;


import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public class PhysicalAddressBuilder {
    private String address;
    private String fullName;
    private String zip;
    private String foreignState;
    
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

    public PhysicalAddressBuilder withZip(String zip) {
        this.zip = zip;
        return this;
    }

    public PhysicalAddressBuilder withForeignState(String foreignState) {
        this.foreignState = foreignState;
        return this;
    }
    
    public PhysicalAddressInt build() {
        if(zip == null){
            zip = "00100";
        }
        
        if(foreignState == null){
            foreignState = "IT";
        }
        
        return PhysicalAddressInt.builder()
                .fullname(fullName)
                .at("Presso")
                .address(address)
                .zip(zip)
                .municipality("Roma")
                .province("RM")
                .foreignState("IT")
                .addressDetails("Scala A")
                .build();
    }
}
