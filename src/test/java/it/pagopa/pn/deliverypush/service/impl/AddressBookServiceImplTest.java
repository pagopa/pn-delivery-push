package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

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
                 
        Mockito.when(userAttributesClient.getLegalAddressBySender(Mockito.anyString(), Mockito.anyString())).thenReturn(
                ResponseEntity.ok(listLegalDigitalAddresses)
        );
        
        //WHEN
        Optional<DigitalAddress> platformAddressOpt =  addressBookService.getPlatformAddresses("TAXID", "SENDERID");
        
        //THEN
        Assertions.assertTrue(platformAddressOpt.isPresent());
        DigitalAddress platformAddress = platformAddressOpt.get();
        Assertions.assertEquals(legalDigitalAddress.getValue(), platformAddress.getAddress());
        Assertions.assertEquals(legalDigitalAddress.getChannelType().getValue(), platformAddress.getType().getValue());
    }

    @Test
    void getCourtesyAddress() {
        //GIVEN
        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue("indirizzo@prova.com");

        List<CourtesyDigitalAddress> listLegalDigitalAddresses = Collections.singletonList(courtesyDigitalAddress);

        Mockito.when(userAttributesClient.getCourtesyAddressBySender(Mockito.anyString(), Mockito.anyString())).thenReturn(
                ResponseEntity.ok(listLegalDigitalAddresses)
        );

        //WHEN
        Optional<List<DigitalAddress>> listCourtesyAddressOpt =  addressBookService.getCourtesyAddress("TAXID", "SENDERID");

        //THEN
        Assertions.assertTrue(listCourtesyAddressOpt.isPresent());
        List<DigitalAddress> listCourtesyAddress = listCourtesyAddressOpt.get();
        Assertions.assertEquals(courtesyDigitalAddress.getValue(), listCourtesyAddress.get(0).getAddress());
    }
    
}