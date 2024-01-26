package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDtov23;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface WebhookEventsService {

    Mono<ProgressResponseElementDtov23> consumeEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, String lastEventId, boolean withDetail);

    Mono<Void> saveEvent(String paId, String eventId, String iun);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
