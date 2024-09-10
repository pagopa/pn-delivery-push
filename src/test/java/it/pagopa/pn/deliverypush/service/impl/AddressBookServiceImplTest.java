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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class AddressBookServiceImplTest {
    private UserAttributesClient userAttributesClient;
    private AddressBookService addressBookService;

    private static final String LEGAL_ADDRESS = "indirizzo@prova.com";
    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:SEND-self:notification-already-delivered";
    
    @BeforeEach
    void setup() {
        userAttributesClient = Mockito.mock( UserAttributesClient.class );

        addressBookService = new AddressBookServiceImpl(
                userAttributesClient
        );

    }
    
    @ParameterizedTest(name = "Test getPlatformAddresses with channelType={0} and address={1}")
    @MethodSource("getLegalChannelTypes")
    void getPlatformAddresses(LegalChannelType channelType, String address) {
        //GIVEN
        LegalDigitalAddress legalDigitalAddress = new LegalDigitalAddress();
        legalDigitalAddress.setValue(address);
        legalDigitalAddress.setChannelType(channelType);
        
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

    private static Stream<Arguments> getLegalChannelTypes() {
        return Stream.of(
                Arguments.of(LegalChannelType.PEC, LEGAL_ADDRESS),
                Arguments.of(LegalChannelType.SERCQ, SERCQ_ADDRESS)

        );
    }
    
}