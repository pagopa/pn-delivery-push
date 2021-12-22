package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.CourtesyMessage.CourtesyMessage;

import java.util.List;

public interface CourtesyMessageHandler {
    void sendCourtesyMessage(String iun, String taxId);

    List<CourtesyMessage> getCourtesyMessages(String iun, String taxId);
}
