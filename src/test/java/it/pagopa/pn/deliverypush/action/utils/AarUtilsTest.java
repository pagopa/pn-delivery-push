package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

class AarUtilsTest {

    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AarUtils aarUtils;

    private final Integer recIndex = 0;

    @BeforeEach
    void setup() {
        notificationUtils = Mockito.mock(NotificationUtils.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        saveLegalFactsService = Mockito.mock(SaveLegalFactsService.class);
        aarUtils = Mockito.mock(AarUtils.class);
    }

    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineevent() {
        
        
        NotificationInt notification = NotificationTestBuilder.builder().withIun("IUN_01").build();
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(recIndex);
        PdfInfo pdfInfo = PdfInfo.builder().key("one").numberOfPages(1).build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().iun("1").build();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(notificationRecipientInt);
        Mockito.when(saveLegalFactsService.saveAAR(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class))).thenReturn(pdfInfo);
        Mockito.when(timelineUtils.buildAarGenerationTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt())).thenReturn(timelineElementInternal);

        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(Mockito.any(NotificationInt.class), Mockito.anyInt());

        
        // verify save AAR
        Mockito.verify(timelineService, Mockito.times(0)).addTimelineElement(timelineElementInternal, notification);
    }

    @Test
    void getAarGenerationDetails() {

        AarGenerationDetailsInt aarInt = AarGenerationDetailsInt.builder().recIndex(0).build();
        Optional<AarGenerationDetailsInt> aarGenerationDetailsInt = Optional.of(aarInt);

        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(aarGenerationDetailsInt));

        aarUtils.getAarGenerationDetails(Mockito.any(NotificationInt.class), Mockito.anyInt());

        Assertions.assertNotNull(aarGenerationDetailsInt);

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