package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PnDataVaultClient {
    ResponseEntity<Void> updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto);

    ResponseEntity<ConfidentialTimelineElementDto> getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);

    ResponseEntity<List<ConfidentialTimelineElementDto>> getNotificationTimelineByIunWithHttpInfo(String iun);
}
