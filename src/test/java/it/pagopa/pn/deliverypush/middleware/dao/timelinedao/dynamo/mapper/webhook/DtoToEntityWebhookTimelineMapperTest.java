package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.webhook;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ServiceLevelInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DtoToEntityWebhookTimelineMapperTest {
    private final  DtoToEntityWebhookTimelineMapper mapper = new  DtoToEntityWebhookTimelineMapper();

    @Test
    void dtoToEntity() {
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE.IUN_ATVR-VRDL-GPQG-202304-J-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .notificationSentAt(Instant.now())
                .paId("paTestMv")
                .timestamp(Instant.now())
                .ingestionTimestamp(Instant.now().plusMillis(1000))
                .eventTimestamp(Instant.now())
                .details(BaseAnalogDetailsInt.builder()
                        .recIndex(0)
                        .physicalAddress(null)
                        .serviceLevel(ServiceLevelInt.REGISTERED_LETTER_890)
                        .sentAttemptMade(0)
                        .build())
                .build();

        WebhookTimelineElementEntity actual = mapper.dtoToEntity(timelineElementInternal);

        assertThat(actual).isNotNull();
        assertThat(actual.getIun()).isEqualTo(timelineElementInternal.getIun());
        assertThat(actual.getTimelineElementId()).isEqualTo(timelineElementInternal.getElementId());
        assertThat(actual.getPaId()).isEqualTo(timelineElementInternal.getPaId());
        assertThat(actual.getNotificationSentAt()).isEqualTo(timelineElementInternal.getNotificationSentAt());
        assertThat(actual.getCategory().name()).isEqualTo(timelineElementInternal.getCategory().name());
        assertThat(actual.getEventTimestamp()).isEqualTo(timelineElementInternal.getEventTimestamp());
        assertThat(actual.getIngestionTimestamp()).isEqualTo(timelineElementInternal.getIngestionTimestamp());

        // verifica details
        BaseAnalogDetailsInt details = (BaseAnalogDetailsInt) timelineElementInternal.getDetails();
        assertThat(actual.getDetails()).isNotNull();
        assertThat(actual.getDetails().getRecIndex()).isEqualTo(details.getRecIndex());
        assertThat(actual.getDetails().getServiceLevel().name()).isEqualTo(details.getServiceLevel().name());
        assertThat(actual.getDetails().getSentAttemptMade()).isEqualTo(details.getSentAttemptMade());
    }

}