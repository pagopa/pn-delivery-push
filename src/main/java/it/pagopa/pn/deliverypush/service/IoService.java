package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;

import java.time.Instant;

public interface IoService {
    SendMessageResponse.ResultEnum sendIOMessage(NotificationInt notification, int recIndex, Instant schedulingAnalogDate);
}
