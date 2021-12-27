package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;

public interface LegalFactGeneratorService {
    void sendAckLegaclFact(Notification notification);

    void workflowStep(Notification notification);

    void nonDeliveryMessage(Notification notification);

    void receivedMessage(Notification notification);
}
