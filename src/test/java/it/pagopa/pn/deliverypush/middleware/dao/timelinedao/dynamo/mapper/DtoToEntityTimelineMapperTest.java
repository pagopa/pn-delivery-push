package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.BaseAnalogDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactCategoryEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DtoToEntityTimelineMapperTest {

    @Test
    void dtoToEntity() {
        DtoToEntityTimelineMapper dto = new DtoToEntityTimelineMapper();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();
        TimelineElementEntity actual = dto.dtoToEntity(timelineElementInternal);

        assertThat(actual).isNotNull();
        assertThat(actual.getIun()).isEqualTo(timelineElementInternal.getIun());
        assertThat(actual.getTimelineElementId()).isEqualTo(timelineElementInternal.getElementId());
        assertThat(actual.getPaId()).isEqualTo(timelineElementInternal.getPaId());
        assertThat(actual.getNotificationSentAt()).isEqualTo(timelineElementInternal.getNotificationSentAt());
        assertThat(actual.getCategory().name()).isEqualTo(timelineElementInternal.getCategory().name());
        assertThat(actual.getTimestamp()).isEqualTo(timelineElementInternal.getTimestamp());

        // verifica details
        NotificationViewedDetailsInt details = (NotificationViewedDetailsInt) timelineElementInternal.getDetails();
        assertThat(actual.getDetails()).isNotNull();
        assertThat(actual.getDetails().getRecIndex()).isEqualTo(details.getRecIndex());
        assertThat(actual.getDetails().getNotificationCost()).isEqualTo(details.getNotificationCost());

        // verifica legalFacts
        assertThat(actual.getLegalFactIds()).isNotNull().hasSize(timelineElementInternal.getLegalFactsIds().size());
        assertThat(actual.getLegalFactIds().get(0).getKey()).isEqualTo(timelineElementInternal.getLegalFactsIds().get(0).getKey());
        assertThat(actual.getLegalFactIds().get(0).getCategory().name()).isEqualTo(timelineElementInternal.getLegalFactsIds().get(0).getCategory().name());
    }

    @Test
    void dtoToEntityPaid() {
        DtoToEntityTimelineMapper dto = new DtoToEntityTimelineMapper();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("NOTIFICATION_PAID.IUN_MPKG-MHLY-GXHE-202301-P-1.CODE_PPA30229167420586447277777777777")
                .category(TimelineElementCategoryInt.PAYMENT)
                .notificationSentAt(Instant.now())
                .paId("aa6e8c72-7944-4dcd-8668-f596447fec6d")
                .timestamp(Instant.now())
                .details(NotificationPaidDetails.builder()
                        .creditorTaxId("creditorTaxId")
                        .noticeCode("noticeId")
                        .recipientType("PF")
                        .amount(1200)
                        .recIndex(0)
                        .paymentSourceChannel("PA")
                        .build())
                .build();

        TimelineElementEntity actual = dto.dtoToEntity(timelineElementInternal);

        assertThat(actual).isNotNull();
        assertThat(actual.getIun()).isEqualTo(timelineElementInternal.getIun());
        assertThat(actual.getTimelineElementId()).isEqualTo(timelineElementInternal.getElementId());
        assertThat(actual.getPaId()).isEqualTo(timelineElementInternal.getPaId());
        assertThat(actual.getNotificationSentAt()).isEqualTo(timelineElementInternal.getNotificationSentAt());
        assertThat(actual.getCategory().name()).isEqualTo(timelineElementInternal.getCategory().name());
        assertThat(actual.getTimestamp()).isEqualTo(timelineElementInternal.getTimestamp());

        // verifica details
        NotificationPaidDetails details = (NotificationPaidDetails) timelineElementInternal.getDetails();
        assertThat(actual.getDetails()).isNotNull();
        assertThat(actual.getDetails().getRecIndex()).isEqualTo(details.getRecIndex());
        assertThat(actual.getDetails().getCreditorTaxId()).isEqualTo(details.getCreditorTaxId());
        assertThat(actual.getDetails().getNoticeCode()).isEqualTo(details.getNoticeCode());
        assertThat(actual.getDetails().getRecipientType()).isEqualTo(details.getRecipientType());
        assertThat(actual.getDetails().getPaymentSourceChannel()).isEqualTo(details.getPaymentSourceChannel());
        assertThat(actual.getDetails().getAmount()).isEqualTo(details.getAmount());

    }

    @Test
    void dtoToEntityPrepareAnalogDomicile_ServiceLevel_AR_REGISTERED_LETTER() {
        DtoToEntityTimelineMapper dto = new DtoToEntityTimelineMapper();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE.IUN_ATVR-VRDL-GPQG-202304-J-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .notificationSentAt(Instant.now())
                .paId("paTestMv")
                .timestamp(Instant.now())
                .details(BaseAnalogDetailsInt.builder()
                        .recIndex(0)
                        .physicalAddress(null)
                        .serviceLevel(ServiceLevelInt.AR_REGISTERED_LETTER)
                        .sentAttemptMade(0)
                        .build())
                .build();

        TimelineElementEntity actual = dto.dtoToEntity(timelineElementInternal);

        assertThat(actual).isNotNull();
        assertThat(actual.getIun()).isEqualTo(timelineElementInternal.getIun());
        assertThat(actual.getTimelineElementId()).isEqualTo(timelineElementInternal.getElementId());
        assertThat(actual.getPaId()).isEqualTo(timelineElementInternal.getPaId());
        assertThat(actual.getNotificationSentAt()).isEqualTo(timelineElementInternal.getNotificationSentAt());
        assertThat(actual.getCategory().name()).isEqualTo(timelineElementInternal.getCategory().name());
        assertThat(actual.getTimestamp()).isEqualTo(timelineElementInternal.getTimestamp());

        // verifica details
        BaseAnalogDetailsInt details = (BaseAnalogDetailsInt) timelineElementInternal.getDetails();
        assertThat(actual.getDetails()).isNotNull();
        assertThat(actual.getDetails().getRecIndex()).isEqualTo(details.getRecIndex());
        assertThat(actual.getDetails().getServiceLevel().name()).isEqualTo(details.getServiceLevel().name());
        assertThat(actual.getDetails().getSentAttemptMade()).isEqualTo(details.getSentAttemptMade());
    }

    @Test
    void dtoToEntityPrepareAnalogDomicile_ServiceLevel_REGISTERED_LETTER_890() {
        DtoToEntityTimelineMapper dto = new DtoToEntityTimelineMapper();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE.IUN_ATVR-VRDL-GPQG-202304-J-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .notificationSentAt(Instant.now())
                .paId("paTestMv")
                .timestamp(Instant.now())
                .details(BaseAnalogDetailsInt.builder()
                        .recIndex(0)
                        .physicalAddress(null)
                        .serviceLevel(ServiceLevelInt.REGISTERED_LETTER_890)
                        .sentAttemptMade(0)
                        .build())
                .build();

        TimelineElementEntity actual = dto.dtoToEntity(timelineElementInternal);

        assertThat(actual).isNotNull();
        assertThat(actual.getIun()).isEqualTo(timelineElementInternal.getIun());
        assertThat(actual.getTimelineElementId()).isEqualTo(timelineElementInternal.getElementId());
        assertThat(actual.getPaId()).isEqualTo(timelineElementInternal.getPaId());
        assertThat(actual.getNotificationSentAt()).isEqualTo(timelineElementInternal.getNotificationSentAt());
        assertThat(actual.getCategory().name()).isEqualTo(timelineElementInternal.getCategory().name());
        assertThat(actual.getTimestamp()).isEqualTo(timelineElementInternal.getTimestamp());

        // verifica details
        BaseAnalogDetailsInt details = (BaseAnalogDetailsInt) timelineElementInternal.getDetails();
        assertThat(actual.getDetails()).isNotNull();
        assertThat(actual.getDetails().getRecIndex()).isEqualTo(details.getRecIndex());
        assertThat(actual.getDetails().getServiceLevel().name()).isEqualTo(details.getServiceLevel().name());
        assertThat(actual.getDetails().getSentAttemptMade()).isEqualTo(details.getSentAttemptMade());

    }

    private TimelineElementInternal buildTimelineElementInternal() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        TimelineElementDetailsInt elementDetailsInt = parseDetailsFromEntity(TimelineElementDetailsEntity.builder()
                .recIndex(0)
                .notificationCost(100)
                .build(), TimelineElementCategoryInt.NOTIFICATION_VIEWED);

        LegalFactsIdInt legalFactsIdInt = buildLegalFactsIdInt();
        List<LegalFactsIdInt> legalFactsIdInts = new ArrayList<>();
        legalFactsIdInts.add(legalFactsIdInt);

        return TimelineElementInternal.builder()
                .iun("001")
                .elementId("002")
                .paId("003")
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .timestamp(instant)
                .details(elementDetailsInt)
                .legalFactsIds(legalFactsIdInts)
                .notificationSentAt(Instant.now())
                .build();
    }

    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId) {
        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details(details)
                .build();
    }

    private TimelineElementDetailsInt parseDetailsFromEntity(TimelineElementDetailsEntity entity, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(entity, category.getDetailsJavaClass());
    }

    private LegalFactsIdInt buildLegalFactsIdInt() {
        return LegalFactsIdInt.builder()
                .key("001")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build();
    }

    private List<LegalFactsIdEntity> convertLegalFactsToEntity(List<LegalFactsIdInt> dto) {
        List<LegalFactsIdEntity> legalFactsIds = null;

        if (dto != null) {
            legalFactsIds = dto.stream().map(this::mapOneLegalFact).collect(Collectors.toList());
        }

        return legalFactsIds;
    }

    private LegalFactsIdEntity mapOneLegalFact(LegalFactsIdInt legalFactsId) {
        LegalFactsIdEntity entity = new LegalFactsIdEntity();
        entity.setKey(legalFactsId.getKey());
        entity.setCategory(LegalFactCategoryEntity.valueOf(legalFactsId.getCategory().getValue()));
        return entity;
    }
}