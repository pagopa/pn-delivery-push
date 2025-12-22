package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;

import java.io.IOException;
import java.time.Instant;

public interface LegalFactGenerator {

    /**
     * Generates the legal fact for a viewed notification.
     *
     * @param iun the unique identifier of the notification.
     * @param recipient the recipient of the notification.
     * @param delegateInfo additional delegate information (if any).
     * @param timeStamp the timestamp when the notification was viewed.
     * @param notification the notification object containing details about the notification.
     * @return a byte array representing the pdf legal fact for the viewed notification.
     */
    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) throws IOException;
}
