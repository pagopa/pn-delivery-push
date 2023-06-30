package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalChannelType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class AddressBookServiceImplTest {
    private UserAttributesClient userAttributesClient;
    private AddressBookService addressBookService;
    
    @BeforeEach
    void setup() {
        userAttributesClient = Mockito.mock( UserAttributesClient.class );

        addressBookService = new AddressBookServiceImpl(
                userAttributesClient
        );

    }
    
    @Test
    void getPlatformAddresses() {
        //GIVEN
        LegalDigitalAddress legalDigitalAddress = new LegalDigitalAddress();
        legalDigitalAddress.setValue("indirizzo@prova.com");
        legalDigitalAddress.setChannelType(LegalChannelType.PEC);
        
        List<LegalDigitalAddress> listLegalDigitalAddresses = Collections.singletonList(legalDigitalAddress);
                 
        Mockito.when(userAttributesClient.getLegalAddressBySender(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(listLegalDigitalAddresses);
        
        //WHEN
        Optional<LegalDigitalAddressInt> platformAddressOpt =  addressBookService.getPlatformAddresses("TAXIDANON", "SENDERID");
        
        //THEN
        Assertions.assertTrue(platformAddressOpt.isPresent());
        LegalDigitalAddressInt platformAddress = platformAddressOpt.get();
        Assertions.assertEquals(legalDigitalAddress.getValue(), platformAddress.getAddress());
        Assertions.assertEquals(legalDigitalAddress.getChannelType().getValue(), platformAddress.getType().getValue());
    }

    @Test
    void getCourtesyAddress() {
        //GIVEN
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue("indirizzo@prova.com");
        courtesyDigitalAddress.setChannelType(CourtesyChannelType.EMAIL);

        List<CourtesyDigitalAddress> listLegalDigitalAddresses = Collections.singletonList(courtesyDigitalAddress);

        Mockito.when(userAttributesClient.getCourtesyAddressBySender(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(listLegalDigitalAddresses);

        //WHEN
        Optional<List<CourtesyDigitalAddressInt>> listCourtesyAddressOpt =  addressBookService.getCourtesyAddress("TAXIDANON", "SENDERID");

        //THEN
        Assertions.assertTrue(listCourtesyAddressOpt.isPresent());
        List<CourtesyDigitalAddressInt> listCourtesyAddress = listCourtesyAddressOpt.get();
        Assertions.assertEquals(courtesyDigitalAddress.getValue(), listCourtesyAddress.get(0).getAddress());
    }
    
}