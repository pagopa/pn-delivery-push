package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.TimelineElementV23;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


class TimelineElementWebhookMapperTest {

    @Test
    void fromInternalToExternal() {
        TimelineElementCategoryInt category = TimelineElementCategoryInt.REQUEST_ACCEPTED;
        String elementId = "elementId";
        Instant instant = Instant.now();
        LegalFactsIdInt legalFactsIdInt = getLegalFactsIdInt(LegalFactCategoryInt.DIGITAL_DELIVERY);
        TimelineElementInternal timelineElementDetailsInt = getTimelineElementInternal(category, elementId, instant, legalFactsIdInt);

        TimelineElementV23 timelineElementV23 = TimelineElementWebhookMapper.internalToExternal(timelineElementDetailsInt);
        Assertions.assertNotNull(timelineElementV23);
        Assertions.assertEquals(category.getValue(), timelineElementV23.getCategory().getValue());
        Assertions.assertEquals(elementId, timelineElementV23.getElementId());
        Assertions.assertEquals(instant, timelineElementV23.getTimestamp());
        Assertions.assertEquals(legalFactsIdInt.getCategory().getValue(), timelineElementV23.getLegalFactsIds().get(0).getCategory().getValue());
        Assertions.assertEquals(legalFactsIdInt.getKey(), timelineElementV23.getLegalFactsIds().get(0).getKey());

        legalFactsIdInt = getLegalFactsIdInt(null);
        timelineElementDetailsInt = getTimelineElementInternal(category, elementId, instant, legalFactsIdInt);
        timelineElementDetailsInt.setLegalFactsIds(null);
        timelineElementV23 = TimelineElementWebhookMapper.internalToExternal(timelineElementDetailsInt);
        Assertions.assertNotNull(timelineElementV23);
        Assertions.assertNull(timelineElementV23.getLegalFactsIds());
    }

    private LegalFactsIdInt getLegalFactsIdInt(LegalFactCategoryInt categoryInt) {
        return LegalFactsIdInt.builder()
                .key("key")
                .category(categoryInt)
                .build();
    }

    private TimelineElementInternal getTimelineElementInternal(TimelineElementCategoryInt category, String elementId, Instant instant, LegalFactsIdInt legalFactsIdInt) {
        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("municipality")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();

        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(legalFactsIdInt);

        return TimelineElementInternal.builder()
                .category(category)
                .elementId(elementId)
                .timestamp(instant)
                .details(details)
                .legalFactsIds(legalFactsIds)
                .build();
    }
}