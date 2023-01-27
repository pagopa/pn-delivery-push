package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AuditLogService;
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
    private AuditLogService auditLogService;
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
        auditLogService = Mockito.mock(AuditLogService.class);
        aarUtils = new AarUtils(timelineService, timelineUtils, saveLegalFactsService, notificationUtils, auditLogService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineeventFailed() {

        String msg = "PN_DELIVERYPUSH_GENERATEPDFFAILED";
        NotificationInt notificationInt = newNotification();
        String quickAccessToken = "test";
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_NT_AAR), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        PnInternalException exception = Assertions.assertThrows(PnInternalException.class, () -> {
            aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notificationInt, recIndex, quickAccessToken);
        });

        Assertions.assertEquals(msg, exception.getProblem().getErrors().get(0).getCode());
        Mockito.verify( auditLogEvent).generateFailure(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineeventIsPresent() {

        NotificationInt notificationInt = newNotification();
        Optional<TimelineElementInternal> timeline = Optional.of(newTimelineElementInternal());
        String elementId = "IUN_01_aar_gen_0";
        String quickAccessToken = "test";

        Mockito.when(timelineService.getTimelineElement(notificationInt.getIun(), elementId)).thenReturn(timeline);

        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notificationInt, recIndex, quickAccessToken);

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(newTimelineElementInternal(), notificationInt);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAARAndSaveInSafeStorageAndAddTimelineeventSuccess() {

        NotificationInt notificationInt = newNotification();
        Optional<TimelineElementInternal> timeline = Optional.empty();
        String elementId = "IUN_01_aar_gen_0";
        String quickAccessToken = "test";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_NT_AAR), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(saveLegalFactsService.saveAAR(notificationInt, recipient, quickAccessToken)).thenReturn(PdfInfo.builder().key("001").build());
        Mockito.when(timelineService.getTimelineElement(notificationInt.getIun(), elementId)).thenReturn(timeline);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notificationInt, recIndex, quickAccessToken);

        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any( NotificationInt.class ));
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any(), Mockito.any());
    }

    @Test
    void getAarGenerationDetails() {

        NotificationInt notificationInt = newNotification();
        AarGenerationDetailsInt aarInt = AarGenerationDetailsInt.builder().recIndex(0).generatedAarUrl("http://test").numberOfPages(2).build();
        Optional<AarGenerationDetailsInt> aarGenerationDetailsInt = Optional.of(aarInt);

        Mockito.when(timelineService.getTimelineElementDetails(notificationInt.getIun(), "IUN_01_aar_gen_0", AarGenerationDetailsInt.class)).thenReturn(aarGenerationDetailsInt);

        AarGenerationDetailsInt tmp = aarUtils.getAarGenerationDetails(notificationInt, recIndex);

        Assertions.assertEquals(tmp, aarGenerationDetailsInt.get());
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