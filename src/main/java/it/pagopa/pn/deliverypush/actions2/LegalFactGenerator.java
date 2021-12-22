package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.Notification;

public interface LegalFactGenerator {
    void sendeAckLegaclFact(Notification notification);

    void conclusionStep(Notification notification);

    void nonDeliveryMessage(Notification notification);

    void receivedMessage(Notification notification);
}
