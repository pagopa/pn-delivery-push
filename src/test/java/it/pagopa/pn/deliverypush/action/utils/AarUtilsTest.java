package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.Optional;

class AarUtilsTest {

    @Mock
    private NotificationUtils notificationUtils;
    @MockBean
    private TimelineService timelineService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @MockBean
    private TimelineUtils timelineUtils;
    
    private final Integer recIndex = 0;
    
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineevent() {
        // GIVEN
        NotificationInt notification = getNotification();
        
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(recIndex);
        Mockito.when(saveLegalFactsService.saveAAR(notification, notificationRecipientInt)).thenReturn(PdfInfo.builder().build());
        TimelineElementInternal timelineEvent = timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, Mockito.anyString(), Mockito.anyInt());

        // WHEN
        timelineService.addTimelineElement(timelineEvent, notification);
        
        // THEN
        Assertions.assertNotNull(timelineEvent);
        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any( NotificationInt.class ));
    }

    @Test
    void getAarGenerationDetails() {
        //GIVEN
        NotificationInt notification = getNotification();
        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
        
        // WHEN
        Optional<AarGenerationDetailsInt> detailOpt =
                timelineService.getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetailsInt.class);
        
        // THEN
        Assertions.assertTrue(detailOpt.isPresent());
    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}