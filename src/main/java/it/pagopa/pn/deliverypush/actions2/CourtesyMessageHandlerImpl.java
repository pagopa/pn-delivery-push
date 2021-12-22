package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.CourtesyMessage.CourtesyMessage;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;

import java.time.Instant;
import java.util.List;

public class CourtesyMessageHandlerImpl implements CourtesyMessageHandler {
    private AddressBook addressBook;

    @Override
    public void sendCourtesyMessage(String iun, String taxId) {
        addressBook.getAddresses(taxId)
                .ifPresent(addressBookItem -> {
                    addressBookItem.getCourtesyAddresses().forEach(courtesyAddress -> {
                        if (courtesyAddress.getAddress() != null) {
                            //TODO Effettuare invio messaggio di cortesia

                            CourtesyMessage courtesyMessage = CourtesyMessage.builder()
                                    .taxId(taxId)
                                    .address(courtesyAddress.getAddress())
                                    .iun(iun)
                                    .insertDate(Instant.now()).build();
                            //Salvare a DB il messaggio di cortesia inviato
                        }

                    });
                });
    }

    @Override
    public List<CourtesyMessage> getCourtesyMessages(String iun, String taxId) {
        //TODO Implementare getMessaggiDalDb
        return null;
    }
}
