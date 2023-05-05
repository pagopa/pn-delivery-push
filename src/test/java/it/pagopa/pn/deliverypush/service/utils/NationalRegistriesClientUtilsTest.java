package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

class NationalRegistriesClientUtilsTest {

    private TimelineService timelineService;

    private TimelineUtils timelineUtils;

    private PublicRegistryUtils publicRegistryUtils;

    @BeforeEach
    public void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        publicRegistryUtils = new PublicRegistryUtils(timelineService, timelineUtils);
    }

    @Test
    void generateCorrelationId() {

        String expected = TimelineEventId.NATIONAL_REGISTRY_CALL.buildEventId(
                EventId.builder()
                        .iun("001")
                        .recIndex(1)
                        .deliveryMode(DeliveryModeInt.DIGITAL)
                        .contactPhase(ContactPhaseInt.SEND_ATTEMPT)
                        .sentAttemptMade(1)
                        .build());

        String actual = publicRegistryUtils.generateCorrelationId("001", 1, ContactPhaseInt.SEND_ATTEMPT, 1, DeliveryModeInt.DIGITAL);

        Assertions.assertEquals(actual, expected);
    }

    @Test
    void addPublicRegistryCallToTimeline() {

        NotificationInt notification = buildNotificationInt("001");

        publicRegistryUtils.addPublicRegistryCallToTimeline(notification, 1, ContactPhaseInt.CHOOSE_DELIVERY, 1, "001", DeliveryModeInt.DIGITAL, null);

        Mockito.verify(timelineUtils, Mockito.times(1)).buildPublicRegistryCallTimelineElement(notification, 1, "001", DeliveryModeInt.DIGITAL, ContactPhaseInt.CHOOSE_DELIVERY, 1, null);
    }

    @Test
    void getPublicRegistryCallDetail() {

        PublicRegistryCallDetailsInt publicRegistryCallDetails = PublicRegistryCallDetailsInt.builder()
                .contactPhase(ContactPhaseInt.SEND_ATTEMPT)
                .deliveryMode(DeliveryModeInt.DIGITAL)
                .sentAttemptMade(1)
                .recIndex(1)
                .build();

        Mockito.when(timelineService.getTimelineElementDetails("001", "001", PublicRegistryCallDetailsInt.class)).thenReturn(Optional.of(publicRegistryCallDetails));

        PublicRegistryCallDetailsInt response = publicRegistryUtils.getPublicRegistryCallDetail("001", "001");

        Assertions.assertEquals(response, publicRegistryCallDetails);
    }

    @Test
    void addPublicRegistryResponseToTimeline() {
        NationalRegistriesResponse nationalRegistriesResponse =
                NationalRegistriesResponse.builder()
                        .correlationId("001" + "_" + "001" + "1121")
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("account@dominio.it")
                                .build()).build();


        publicRegistryUtils.addPublicRegistryResponseToTimeline(buildNotificationInt("001"), 1, nationalRegistriesResponse);

        Mockito.verify(timelineUtils, Mockito.times(1)).buildPublicRegistryResponseCallTimelineElement(buildNotificationInt("001"), 1, nationalRegistriesResponse);
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
}