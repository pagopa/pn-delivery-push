package it.pagopa.pn.deliverypush.action.choosedeliverymode;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;

import java.time.Instant;
import java.util.Optional;

public interface ChooseDeliveryModeUtils {

    void addAvailabilitySourceToTimeline(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt addressSource, boolean isAvailable);

    void addScheduleAnalogWorkflowToTimeline(Integer recIndex, NotificationInt notification, Instant schedulingDate);

    Optional<SendCourtesyMessageDetailsInt> getFirstSentCourtesyMessage(String iun, Integer recIndex);

    Optional<LegalDigitalAddressInt> getPlatformAddress(NotificationInt notification, Integer recIndex);

    LegalDigitalAddressInt getDigitalDomicile(NotificationInt notification, Integer recIndex);

    Optional<LegalDigitalAddressInt> retrievePlatformAddress(NotificationInt notification, Integer recIndex);

    LegalDigitalAddressInt retrieveSpecialAddress(NotificationInt notification, Integer recIndex);

}