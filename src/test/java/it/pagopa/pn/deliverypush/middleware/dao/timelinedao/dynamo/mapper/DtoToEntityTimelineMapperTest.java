package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactCategoryEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DtoToEntityTimelineMapperTest {

    @Test
    void dtoToEntity() {
        DtoToEntityTimelineMapper dto = new DtoToEntityTimelineMapper();

        TimelineElementEntity actual = dto.dtoToEntity(buildTimelineElementInternal());

        Assertions.assertEquals("001", actual.getIun());
    }

    private TimelineElementInternal buildTimelineElementInternal() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        TimelineElementInternal timelineElementDetailsInt = getSendPaperFeedbackTimelineElement("1", "1");
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