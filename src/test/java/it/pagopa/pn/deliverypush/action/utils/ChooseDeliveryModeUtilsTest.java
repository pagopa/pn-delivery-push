package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ChooseDeliveryModeUtilsTest {
    
    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private CourtesyMessageUtils courtesyMessageUtils;
    
    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        courtesyMessageUtils = Mockito.mock(CourtesyMessageUtils.class);
    }

    @Test
    void addAvailabilitySourceToTimeline() {
        
    }

    @Test
    void addScheduleAnalogWorkflowToTimeline() {
    }

    @Test
    void getFirstSentCourtesyMessage() {
    }

    @Test
    void getPlatformAddress() {
    }

    @Test
    void getDigitalDomicile() {
    }

    private NotificationInt getNotificationWithAttachment() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .documents(
                        Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref( NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .build()
                ))
                .build();
    }
}