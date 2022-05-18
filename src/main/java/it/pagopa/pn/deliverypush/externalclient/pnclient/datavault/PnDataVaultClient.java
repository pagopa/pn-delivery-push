package it.pagopa.pn.deliverypush.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import org.springframework.http.ResponseEntity;

public interface PnDataVaultClient {
    ResponseEntity<Void> updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto);

    ResponseEntity<ConfidentialTimelineElementDto> getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);
}
