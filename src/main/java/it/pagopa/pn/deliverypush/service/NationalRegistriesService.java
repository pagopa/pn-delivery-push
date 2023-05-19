package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;

public interface NationalRegistriesService {
    String GET_DIGITAL_GENERAL_ADDRESS = "GET DIGITAL GENERAL ADDRESS";

    void sendRequestForGetDigitalGeneralAddress(NotificationInt notification,
                                                Integer recIndex, 
                                                ContactPhaseInt contactPhase,
                                                int sentAttemptMade,
                                                String relatedFeedbackTimelineId);

    CheckTaxIdOKInt checkTaxId(String taxId);
}
