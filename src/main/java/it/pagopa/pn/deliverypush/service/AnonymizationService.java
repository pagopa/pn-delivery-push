package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AnonymizationService {
    void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDtoInt confidentialTimelineElementDto);
    
    ConfidentialTimelineElementDtoInt getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId);
    
    ResponseEntity<List<BaseRecipientDtoInt>> getRecipientDenominationByInternalId(List<String> internalId);
}
