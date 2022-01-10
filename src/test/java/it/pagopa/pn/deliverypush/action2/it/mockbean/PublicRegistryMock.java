package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import org.springframework.context.annotation.Lazy;

public class PublicRegistryMock implements PublicRegistry {
    PublicRegistryResponseHandler publicRegistryResponseHandler;

    public PublicRegistryMock(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        PublicRegistryResponse response;
        if (taxId.contains(TestUtils.PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS)) {
            //Simulazione casistica registri pubblici non riescono a fornire indirizzo generale
            response = PublicRegistryResponse.builder()
                    .correlationId(correlationId)
                    .digitalAddress(null)
                    .build();
        } else {
            //Simulazione casistica registri pubblici riescono a fornire indirizzo generale
            response = PublicRegistryResponse.builder()
                    .correlationId(correlationId)
                    .digitalAddress(DigitalAddress.builder()
                            .type(DigitalAddressType.PEC)
                            .address("Via di prova 10")
                            .build())
                    .build();
        }

        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        PublicRegistryResponse response;
        if (taxId.contains(TestUtils.PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS)) {
            //In questo caso public registry non è riuscito a fornire l'indirizzo
            response = PublicRegistryResponse.builder()
                    .correlationId(correlationId)
                    .physicalAddress(null)
                    .build();
        } else {
            if (taxId.contains(TestUtils.PUBLIC_REGISTRY_OK_GET_ANALOG_ADDRESS_WITH_FAILURE_ADDRESS)) {
                //In questo caso è stata ottenuta la risposta positiva da public registry ma l'indirizzo fornito sarà di una tipologia di fallimmento per external channel
                response = PublicRegistryResponse.builder()
                        .correlationId(correlationId)
                        .physicalAddress(PhysicalAddress.builder()
                                .at("Presso")
                                .address("Via nuova 26 - " + TestUtils.EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT)
                                .zip("00100")
                                .municipality("Roma")
                                .province("RM")
                                .foreignState("IT")
                                .addressDetails("Scala A")
                                .build())
                        .build();
            } else {
                //In questo caso è stata ottenuta la risposta positiva da public registry e l'indirizzo fornito sarà di una tipologia di successo per external channel
                response = PublicRegistryResponse.builder()
                        .correlationId(correlationId)
                        .physicalAddress(PhysicalAddress.builder()
                                .at("Presso")
                                .address("Via nuova 26")
                                .zip("00100")
                                .municipality("Roma")
                                .province("RM")
                                .foreignState("IT")
                                .addressDetails("Scala A")
                                .build())
                        .build();
            }
        }
        publicRegistryResponseHandler.handleResponse(response);
    }
}
