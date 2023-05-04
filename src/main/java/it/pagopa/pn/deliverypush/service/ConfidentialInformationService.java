package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ConfidentialInformationService {
    
    void saveTimelineConfidentialInformation(TimelineElementInternal timelineElementInternal);
    
    Optional<ConfidentialTimelineElementDtoInt> getTimelineElementConfidentialInformation(String iun, String timelineElementId);

    Optional<Map<String, ConfidentialTimelineElementDtoInt>> getTimelineConfidentialInformation(String iun);

    Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId);

    Mono<Void> updateNotificationAddresses(String iun, Boolean normalized, List<NotificationRecipientAddressesDtoInt> list);
}
