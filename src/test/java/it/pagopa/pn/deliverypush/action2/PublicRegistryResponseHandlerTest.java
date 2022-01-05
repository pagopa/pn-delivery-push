package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicRegistryResponseHandlerTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryHandler;
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private TimelineUtils timelineUtils;

    private PublicRegistryResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new PublicRegistryResponseHandler(timelineService, chooseDeliveryHandler, digitalWorkFlowHandler,
                analogWorkflowHandler, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Choose() {
        String iun = "iun01";
        String taxId = "taxId01";
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
                .taxId(taxId).build();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(publicRegistryCallDetails));

        handler.handleResponse(response);

        Mockito.verify(timelineUtils).buildPublicRegistryResponseCallTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(chooseDeliveryHandler).handleGeneralAddressResponse(Mockito.any(PublicRegistryResponse.class), iunCaptor.capture(), Mockito.anyString());

        Assertions.assertEquals(iun, iunCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Sent_digital() {
        String iun = "iun01";
        String taxId = "taxId01";
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
                .taxId(taxId).build();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(publicRegistryCallDetails));

        handler.handleResponse(response);

        Mockito.verify(timelineUtils).buildPublicRegistryResponseCallTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(digitalWorkFlowHandler).handleGeneralAddressResponse(Mockito.any(PublicRegistryResponse.class), iunCaptor.capture(), Mockito.anyString(), Mockito.anyInt());

        Assertions.assertEquals(iun, iunCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Sent_Analog() {
        String iun = "iun01";
        String taxId = "taxId01";
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
                .taxId(taxId).build();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(publicRegistryCallDetails));

        handler.handleResponse(response);

        Mockito.verify(timelineUtils).buildPublicRegistryResponseCallTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any(PublicRegistryResponse.class));

        ArgumentCaptor<String> iunCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(analogWorkflowHandler).handlePublicRegistryResponse(iunCaptor.capture(), Mockito.anyString(), Mockito.any(PublicRegistryResponse.class));

        Assertions.assertEquals(iun, iunCaptor.getValue());

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_contactPhase_Error() {
        String iun = "iun01";
        String taxId = "taxId01";
        PublicRegistryResponse response =
                PublicRegistryResponse.builder()
                        .correlationId(iun + "_" + taxId + "1121")
                        .digitalAddress(DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());


        PnInternalException thrown = assertThrows(
                PnInternalException.class,
                () -> handler.handleResponse(response));

        Assertions.assertTrue(thrown.getMessage().contains("There isn't timelineElement"));

    }
}