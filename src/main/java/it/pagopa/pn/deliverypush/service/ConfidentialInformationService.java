package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ConfidentialInformationService {
    
    void saveTimelineConfidentialInformation(TimelineElementInternal timelineElementInternal);
    
    ConfidentialTimelineElementDtoInt getTimelineConfidentialInformation(String iun, String timelineElementId);
    
    ResponseEntity<List<BaseRecipientDtoInt>> getRecipientDenominationByInternalId(List<String> internalId);
}
