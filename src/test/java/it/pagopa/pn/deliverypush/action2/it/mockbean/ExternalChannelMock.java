package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;

public class ExternalChannelMock implements ExternalChannel {
    ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelMock(@Lazy ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }

    @Override
    public void sendNotification(PnExtChnEmailEvent event) {
        //Invio messaggio di cortesia non necessità di risposta da external channel
    }

    @Override
    public void sendNotification(PnExtChnPecEvent event) {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .digitalUsedAddress(
                        DigitalAddress.builder()
                                .address(event.getPayload().getPecAddress())
                                .type(DigitalAddressType.PEC)
                                .build()
                ).notificationDate(Instant.now())
                .iun(event.getPayload().getIun())
                .taxId(event.getPayload().getRecipientTaxId())
                .build();
        externalChannelResponseHandler.extChannelResponseReceiver(extChannelResponse);
    }

    @Override
    public void sendNotification(PnExtChnPaperEvent event) {
        ExtChannelResponse response = ExtChannelResponse.builder()
                .iun(event.getPayload().getIun())
                .notificationDate(Instant.now())
                .analogUsedAddress(event.getPayload().getDestinationAddress())
                .eventId(event.getHeader().getEventId()).build();

        String address = event.getPayload().getDestinationAddress().getAddress();

        if (address.contains(TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT)) {
            //In questo caso la risposta fornita da external channel è negativa
            response = response.toBuilder()
                    .responseStatus(ExtChannelResponseStatus.KO)
                    .build();

            if (address.contains(TestUtils.INVESTIGATION_ADDRESS_PRESENT_FAILURE)) {
                // Risulta presente un nuovo indirizzo dall'investigazione del postino, ma tale indirizzo dovrà fallire nuovamente nell'invio di external channels
                response = response.toBuilder()
                        .analogNewAddressFromInvestigation(
                                PhysicalAddress.builder()
                                        .at("Presso")
                                        .address("Via nuova 14 - " + TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT)
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .addressDetails("Scala A")
                                        .build()
                        ).build();
            } else {
                if (address.contains(TestUtils.INVESTIGATION_ADDRESS_PRESENT_POSITIVE)) {
                    // Risulta presente un nuovo indirizzo dall'investigazione del postino, è tale indirizzo dovrà avere esito positivo nel successivo invio di external channel
                    response = response.toBuilder()
                            .analogNewAddressFromInvestigation(
                                    PhysicalAddress.builder()
                                            .at("Presso")
                                            .address("Via nuova 14")
                                            .zip("00100")
                                            .municipality("Roma")
                                            .province("RM")
                                            .foreignState("IT")
                                            .addressDetails("Scala A")
                                            .build()
                            ).build();
                }
            }
            //Se non entra in nessuno dei due if precedenti, significa che non è presente l'indirizzo dell'investigazione
        } else {
            response = response.toBuilder()
                    .responseStatus(ExtChannelResponseStatus.OK)
                    .build();
        }

        externalChannelResponseHandler.extChannelResponseReceiver(response);

    }
}
