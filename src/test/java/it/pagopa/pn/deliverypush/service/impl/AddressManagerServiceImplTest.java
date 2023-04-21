package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AnalogAddress;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeRequest;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.mapper.AddressManagerMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

class AddressManagerServiceImplTest {

    @Mock
    private AddressManagerClient addressManagerClient;
    
    private final NotificationUtils notificationUtils = new NotificationUtils();

    private AddressManagerService addressManagerService;
    
    @BeforeEach
    void setup() {
        addressManagerService = new AddressManagerServiceImpl(
                addressManagerClient,
                notificationUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void normalizeAddresses() {
        //GIVEN
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId("recipient1")
                .withInternalId("ANON_recipient1")
                .build();
        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(" Via Nuova")
                .build();
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId("recipient2")
                .withInternalId("ANON_recipient2")
                .withPhysicalAddress(paPhysicalAddress2)
                .build();
        PhysicalAddressInt paPhysicalAddress3 = PhysicalAddressBuilder.builder()
                .withAddress(" Via Nuova 2")
                .build();
        NotificationRecipientInt recipient3 = NotificationRecipientTestBuilder.builder()
                .withTaxId("recipient3")
                .withInternalId("ANON_recipient3")
                .withPhysicalAddress(paPhysicalAddress3)
                .build();
        
        NotificationInt notification = NotificationTestBuilder.builder()
                .withPaId("paId01")
                .withNotificationRecipients( List.of(recipient1, recipient2, recipient3) )
                .build();
        
        String corrId = "corrId";
        
        Mockito.when(addressManagerClient.normalizeAddresses(Mockito.any(NormalizeItemsRequest.class)))
                .thenReturn(Mono.just(new AcceptedResponse()));
        
        //WHEN
        addressManagerService.normalizeAddresses(notification, corrId).block();

        //THEN
        ArgumentCaptor<NormalizeItemsRequest> normItemRequestCaptor = ArgumentCaptor.forClass(NormalizeItemsRequest.class);

        Mockito.verify(addressManagerClient).normalizeAddresses(normItemRequestCaptor.capture());

        NormalizeItemsRequest normItemRequest = normItemRequestCaptor.getValue();
        Assertions.assertEquals(corrId, normItemRequest.getCorrelationId() );

        List<NormalizeRequest> listRequest = normItemRequest.getRequestItems();
        
        int numberRecipientWithPhysicalAddress = 2;
        Assertions.assertEquals(numberRecipientWithPhysicalAddress, listRequest.size());
        
        listRequest.forEach(elem -> {
            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, Integer.valueOf(elem.getId()));
            AnalogAddress address = AddressManagerMapper.getAnalogAddressFromPhysical(recipient.getPhysicalAddress());
            Assertions.assertEquals(address, elem.getAddress());
        });
    }
}