package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class AarUtilsTest {

    private static final String TAX_ID = "tax_id";
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
    @Mock    
    private DocumentCreationRequestService documentCreationRequestService;

    private final Integer recIndex = 0;

    @BeforeEach
    void setup() {
        notificationUtils = Mockito.mock(NotificationUtils.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        saveLegalFactsService = Mockito.mock(SaveLegalFactsService.class);
        aarUtils = new AarUtils(saveLegalFactsService, timelineUtils, timelineService, notificationUtils, documentCreationRequestService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineeventFailed() {

        String msg = "PN_DELIVERYPUSH_GENERATEPDFFAILED";
        NotificationInt notificationInt = newNotification();
        String elementId = "IUN_01_AAR_GEN_0";
        String quickAccessToken = "test";

        Mockito.when(timelineService.getTimelineElement(notificationInt.getIun(), elementId)).thenThrow(new PnInternalException("cannot generate AAR pdf", "test"));

        PnInternalException exception = Assertions.assertThrows(PnInternalException.class, () -> {
            aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineEvent(notificationInt, recIndex, quickAccessToken);
        });

        Assertions.assertEquals(msg, exception.getProblem().getErrors().get(0).getCode());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineeventIsPresent() {

        NotificationInt notificationInt = newNotification();
        Optional<TimelineElementInternal> timeline = Optional.of(newTimelineElementInternal());
        String elementId = "AAR_GEN#IUN_IUN_01#RECINDEX_0".replace("#", TimelineEventIdBuilder.DELIMITER);
        String quickAccessToken = "test";

        Mockito.when(timelineService.getTimelineElement(notificationInt.getIun(), elementId)).thenReturn(timeline);

        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineEvent(notificationInt, recIndex, quickAccessToken);

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(newTimelineElementInternal(), notificationInt);
    }

    @Test
    void getAarGenerationDetails() {

        NotificationInt notificationInt = newNotification();
        AarGenerationDetailsInt aarInt = AarGenerationDetailsInt.builder().recIndex(0).generatedAarUrl("http://test").numberOfPages(2).build();
        Optional<AarGenerationDetailsInt> aarGenerationDetailsInt = Optional.of(aarInt);
        String timelineEventId = "AAR_GEN#IUN_IUN_01#RECINDEX_0".replace("#", TimelineEventIdBuilder.DELIMITER);

        Mockito.when(timelineService.getTimelineElementDetails(notificationInt.getIun(), timelineEventId, AarGenerationDetailsInt.class)).thenReturn(aarGenerationDetailsInt);

        AarGenerationDetailsInt tmp = aarUtils.getAarGenerationDetails(notificationInt, recIndex);

        Assertions.assertEquals(tmp, aarGenerationDetailsInt.get());
    }

    @Test
    void getAarCreationRequestDetailsIntOk() {

        NotificationInt notificationInt = newNotification();
        var aarInt = AarCreationRequestDetailsInt.builder()
                .recIndex(0)
                .aarKey("safestorage://PN-AAR-mock.pdf")
                .numberOfPages(2)
                .aarWithRadd(true)
                .build();
        var expectedDetails = Optional.of(aarInt);
        String timelineEventId = "AAR_CREATION_REQUEST#IUN_IUN_01#RECINDEX_0".replace("#", TimelineEventIdBuilder.DELIMITER);

        Mockito.when(timelineService.getTimelineElementDetails(notificationInt.getIun(), timelineEventId, AarCreationRequestDetailsInt.class))
                .thenReturn(expectedDetails);

        Mockito.when(timelineUtils.createEventIdForAarCreationRequest(notificationInt.getIun(), 0))
                .thenReturn(timelineEventId);

        AarCreationRequestDetailsInt actual = aarUtils.getAarCreationRequestDetailsInt(notificationInt, recIndex);

        Assertions.assertEquals(expectedDetails.get(), actual);
    }

    @Test
    void getAarCreationRequestDetailsIntKo() {

        NotificationInt notificationInt = newNotification();
        String timelineEventId = "AAR_CREATION_REQUEST#IUN_IUN_01#RECINDEX_0".replace("#", TimelineEventIdBuilder.DELIMITER);

        Mockito.when(timelineService.getTimelineElementDetails(notificationInt.getIun(), timelineEventId, AarCreationRequestDetailsInt.class))
                .thenReturn(Optional.empty());

        Mockito.when(timelineUtils.createEventIdForAarCreationRequest(notificationInt.getIun(), 0))
                .thenReturn(timelineEventId);

        Assertions.assertThrows(PnInternalException.class, () -> aarUtils.getAarCreationRequestDetailsInt(notificationInt, recIndex));

    }

    private TimelineElementInternal newTimelineElementInternal() {

        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());

        return TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .legalFactsIds(legalFactsIds)
                .build();
    }

    private NotificationInt newNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
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