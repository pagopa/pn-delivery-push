package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

class NationalRegistriesClientResponseHandlerTest {

    private ChooseDeliveryModeHandler chooseDeliveryHandler;

    private DigitalWorkFlowHandler digitalWorkFlowHandler;

    private PublicRegistryUtils publicRegistryUtils;

    private NotificationService notificationService;

    private PublicRegistryResponseHandler handler;

    @BeforeEach
    void setup() {
        chooseDeliveryHandler = Mockito.mock(ChooseDeliveryModeHandler.class);
        digitalWorkFlowHandler = Mockito.mock(DigitalWorkFlowHandler.class);
        publicRegistryUtils = Mockito.mock(PublicRegistryUtils.class);
        notificationService = Mockito.mock(NotificationService.class);

        handler = new PublicRegistryResponseHandler(chooseDeliveryHandler, digitalWorkFlowHandler, publicRegistryUtils, notificationService);
    }

    @Test
    void handleResponseChooseDelivery() {
        String iun = "iun01";
        String correlationId = "NATIONAL_REGISTRY_CALL#IUN_iun01#RECINDEX_1#DELIVERYMODE_DIGITAL#CONTACTPHASE_CHOOSE_DELIVERY#SENTATTEMPTMADE_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        int recIndex = 1;
        NotificationInt notificationInt = buildNotificationInt(iun);
        PublicRegistryResponse response = buildPublicRegistryResponse(correlationId);
        PublicRegistryCallDetailsInt publicRegistryCallDetails = buildPublicRegistryCallDetailsInt(ContactPhaseInt.CHOOSE_DELIVERY, recIndex, DeliveryModeInt.DIGITAL);

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notificationInt);
        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(iun, correlationId)).thenReturn(publicRegistryCallDetails);

        handler.handleResponse(response);

        Mockito.verify(publicRegistryUtils, Mockito.times(1)).addPublicRegistryResponseToTimeline(notificationInt, recIndex, response);
        Mockito.verify(chooseDeliveryHandler, Mockito.times(1)).handleGeneralAddressResponse(response, notificationInt, recIndex);
    }

    @Test
    void handleResponseSendAttemptDigital() {
        String iun = "iun01";
        String correlationId = "NATIONAL_REGISTRY_CALL#IUN_iun01#RECINDEX_1#DELIVERYMODE_DIGITAL#CONTACTPHASE_CHOOSE_DELIVERY#SENTATTEMPTMADE_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        int recIndex = 1;
        NotificationInt notificationInt = buildNotificationInt(iun);
        PublicRegistryResponse response = buildPublicRegistryResponse(correlationId);
        PublicRegistryCallDetailsInt publicRegistryCallDetails = buildPublicRegistryCallDetailsInt(ContactPhaseInt.SEND_ATTEMPT, recIndex, DeliveryModeInt.DIGITAL);

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notificationInt);
        Mockito.when(publicRegistryUtils.getPublicRegistryCallDetail(iun, correlationId)).thenReturn(publicRegistryCallDetails);

        handler.handleResponse(response);

        Mockito.verify(digitalWorkFlowHandler, Mockito.times(1)).handleGeneralAddressResponse(response, notificationInt, publicRegistryCallDetails);
    }


    private NotificationInt buildNotificationInt(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }

    private PublicRegistryResponse buildPublicRegistryResponse(String correlationId) {
        return PublicRegistryResponse.builder()
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("account@dominio.it")
                        .build())
                .correlationId(correlationId)
                .physicalAddress(null)
                .build();
    }

    private PublicRegistryCallDetailsInt buildPublicRegistryCallDetailsInt(ContactPhaseInt contactPhaseInt, int recIndex, DeliveryModeInt deliveryModeInt) {
        return PublicRegistryCallDetailsInt.builder()
                .contactPhase(contactPhaseInt)
                .deliveryMode(deliveryModeInt)
                .sentAttemptMade(1)
                .recIndex(recIndex)
                .build();
    }
}