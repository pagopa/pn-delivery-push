package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;


import ContactPhase;
import DeliveryMode;
import PublicRegistryCallDetails;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

class PublicRegistryResponseHandlerTest {
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryHandler;
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private PublicRegistryUtils publicRegistryUtils;

    private PublicRegistryResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new PublicRegistryResponseHandler(chooseDeliveryHandler,
                digitalWorkFlowHandler, analogWorkflowHandler,
                publicRegistryUtils);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Choose() {
        //GIVEN
        String iun = "iun01";
        String taxId = "taxId01";
        Integer recIndex = 0;
        
        PublicRegistryResponse response =
                PublicRegistryResponse.builder()
                        .correlationId(iun + "_" + taxId + "1121")
                        .digitalAddress(DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        PublicRegistryCallDetails publicRegistryCallDetails = PublicRegistryCallDetails.builder()
                .contactPhase(ContactPhase.CHOOSE_DELIVERY)
                .deliveryMode(null)
                .recIndex(recIndex)
                .build();

        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(publicRegistryCallDetails);
        
        //WHEN
        handler.handleResponse(response);
        
        //THEN
        Mockito.verify(publicRegistryUtils).addPublicRegistryResponseToTimeline(Mockito.anyString(), Mockito.anyInt(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(chooseDeliveryHandler).handleGeneralAddressResponse(Mockito.any(PublicRegistryResponse.class), iunCaptor.capture(), Mockito.anyInt());

        Assertions.assertEquals(iun, iunCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Sent_digital() {
        //GIVEN
        String iun = "iun01";
        String taxId = "taxId01";
        Integer recIndex = 0;

        PublicRegistryResponse response =
                PublicRegistryResponse.builder()
                        .correlationId(iun + "_" + taxId + "1121")
                        .digitalAddress(DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        PublicRegistryCallDetails publicRegistryCallDetails = PublicRegistryCallDetails.builder()
                .contactPhase(ContactPhase.SEND_ATTEMPT)
                .deliveryMode(DeliveryMode.DIGITAL)
                .sentAttemptMade(1)
                .recIndex(recIndex)
                .build();

        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(publicRegistryCallDetails);
        
        //WHEN
        handler.handleResponse(response);
        
        //THEN
        Mockito.verify(publicRegistryUtils).addPublicRegistryResponseToTimeline(Mockito.anyString(), Mockito.anyInt(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(digitalWorkFlowHandler).handleGeneralAddressResponse(Mockito.any(PublicRegistryResponse.class), iunCaptor.capture(), Mockito.any(PublicRegistryCallDetails.class));

        Assertions.assertEquals(iun, iunCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Sent_Analog() {
        //GIVEN
        String iun = "iun01";
        String taxId = "taxId01";
        Integer recIndex = 0;
        
        PublicRegistryResponse response =
                PublicRegistryResponse.builder()
                        .correlationId(iun + "_" + taxId + "1121")
                        .physicalAddress(
                                PhysicalAddress.builder()
                                        .address("testaddress")
                                        .build()
                        ).build();


        PublicRegistryCallDetails publicRegistryCallDetails = PublicRegistryCallDetails.builder()
                .contactPhase(ContactPhase.SEND_ATTEMPT)
                .deliveryMode(DeliveryMode.ANALOG)
                .sentAttemptMade(1)
                .recIndex(recIndex)
                .build();

        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(publicRegistryCallDetails);

        //WHEN
        handler.handleResponse(response);
        
        //THEN
        Mockito.verify(publicRegistryUtils).addPublicRegistryResponseToTimeline(Mockito.anyString(), Mockito.anyInt(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(analogWorkflowHandler).handlePublicRegistryResponse(iunCaptor.capture(), Mockito.anyInt(), Mockito.any(PublicRegistryResponse.class), Mockito.anyInt());

        Assertions.assertEquals(iun, iunCaptor.getValue());

    }
}