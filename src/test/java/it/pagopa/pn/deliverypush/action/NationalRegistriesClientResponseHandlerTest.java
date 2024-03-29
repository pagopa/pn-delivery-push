package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.eq;

class NationalRegistriesClientResponseHandlerTest {
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryHandler;
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private PublicRegistryUtils publicRegistryUtils;
    @Mock
    private NotificationService notificationService;
    @MockBean
    private NotificationProcessCostService notificationProcessCostService;

    private NationalRegistriesResponseHandler handler;

    @BeforeEach
    public void setup() {
        TimelineUtils timelineUtils = new TimelineUtils(Mockito.mock(InstantNowSupplier.class), Mockito.mock(TimelineService.class), notificationProcessCostService);
        handler = new NationalRegistriesResponseHandler(chooseDeliveryHandler,
                digitalWorkFlowHandler,
                publicRegistryUtils, notificationService, timelineUtils);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Choose() {
        //GIVEN
        String iun = "iun01";
        Integer recIndex = 0;
        String correlationId = "NATIONAL_REGISTRY_CALL#IUN_iun01#RECINDEX_0#CONTACTPHASE_CHOOSE_DELIVERY".replace("#", TimelineEventIdBuilder.DELIMITER);
        
        NationalRegistriesResponse response =
                NationalRegistriesResponse.builder()
                        .correlationId(correlationId)
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        PublicRegistryCallDetailsInt publicRegistryCallDetails = PublicRegistryCallDetailsInt.builder()
                .contactPhase(ContactPhaseInt.CHOOSE_DELIVERY)
                .deliveryMode(null)
                .recIndex(recIndex)
                .build();

        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(iun, correlationId))
                .thenReturn(publicRegistryCallDetails);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when( notificationService.getNotificationByIun(iun) ).thenReturn(notification);
        
        //WHEN
        handler.handleResponse(response);
        
        //THEN
        Mockito.verify(publicRegistryUtils).addPublicRegistryResponseToTimeline(notification, recIndex, response);

        ArgumentCaptor<NotificationInt> notificationIntArgumentCaptor = ArgumentCaptor.forClass(NotificationInt.class);

        Mockito.verify(chooseDeliveryHandler).handleGeneralAddressResponse(eq(response), notificationIntArgumentCaptor.capture(), eq(recIndex));

        Assertions.assertEquals(iun, notificationIntArgumentCaptor.getValue().getIun());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleResponse_Sent_digital() {
        //GIVEN
        String iun = "iun01";
        String correlationId = "national_call#IUN_iun01#RECINDEX_0#DELIVERYMODE_DIGITAL#CONTACTPHASE_SEND_ATTEMPT#SENTATTEMPTMADE_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        Integer recIndex = 0;

        NationalRegistriesResponse response =
                NationalRegistriesResponse.builder()
                        .correlationId(correlationId)
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        PublicRegistryCallDetailsInt publicRegistryCallDetails = PublicRegistryCallDetailsInt.builder()
                .contactPhase(ContactPhaseInt.SEND_ATTEMPT)
                .deliveryMode(DeliveryModeInt.DIGITAL)
                .sentAttemptMade(1)
                .recIndex(recIndex)
                .build();

        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(publicRegistryCallDetails);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when( notificationService.getNotificationByIun(iun) ).thenReturn(notification);
        
        //WHEN
        handler.handleResponse(response);
        
        //THEN
        Mockito.verify(publicRegistryUtils).addPublicRegistryResponseToTimeline(notification, recIndex, response);

        ArgumentCaptor<NotificationInt> notificationIntArgumentCaptor = ArgumentCaptor.forClass(NotificationInt.class);

        Mockito.verify(digitalWorkFlowHandler).handleGeneralAddressResponse(eq(response), notificationIntArgumentCaptor.capture(), eq(publicRegistryCallDetails));

        Assertions.assertEquals(iun, notificationIntArgumentCaptor.getValue().getIun());

    }

}