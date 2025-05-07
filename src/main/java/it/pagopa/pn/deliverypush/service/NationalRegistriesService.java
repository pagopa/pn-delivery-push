package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;

import java.util.List;

public interface NationalRegistriesService {
    void sendRequestForGetDigitalGeneralAddress(NotificationInt notification,
                                                Integer recIndex, 
                                                ContactPhaseInt contactPhase,
                                                int sentAttemptMade,
                                                String relatedFeedbackTimelineId);

    CheckTaxIdOKInt checkTaxId(String taxId);

    List<NationalRegistriesResponse> getMultiplePhysicalAddress(NotificationInt notification);
}
