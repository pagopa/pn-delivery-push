package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
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
        //TODO Da completare
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
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .build();
        if (taxId.contains(TestUtils.PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS)) {
            //In questo caso public registry non è riuscito a fornire l'indirizzo
            response = response.toBuilder()
                    .physicalAddress(null)
                    .build();
        } else {
            //In questo caso public registry è riuscito a fornire l'indirizzo
            //La logica per l'indirizzo restituito è presente nel taxId. Viene quindi concatenato il taxId all'address restitutito per permettere alle logiche presenti
            // in ExternalChannelMock di funzionare

            response = PublicRegistryResponse.builder()
                    .correlationId(correlationId)
                    .physicalAddress(
                            TestUtils.getPhysicalAddressWithTaxIdForPublicRegistry(taxId)
                    )
                    .build();
        }

        publicRegistryResponseHandler.handleResponse(response);
    }

}
