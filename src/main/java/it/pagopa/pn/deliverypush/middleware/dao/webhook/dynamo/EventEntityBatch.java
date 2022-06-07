package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class EventEntityBatch {
    private String streamId;
    private Instant lastTimestampRead;
    private List<EventEntity> events;
}
