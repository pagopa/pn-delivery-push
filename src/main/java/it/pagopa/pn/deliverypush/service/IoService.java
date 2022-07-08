package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

import java.time.Instant;

public interface IoService {
    void sendIOMessage(NotificationInt notification, int recIndex, Instant requestAcceptedDate);
}
