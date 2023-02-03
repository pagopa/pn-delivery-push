package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.web.reactive.function.client.WebClient;

public class NationalRegistriesBaseClient extends CommonBaseClient {

    protected static final String PN_NATIONAL_REGISTRIES_CX_ID = "pn-national-registries-cx-id";


    @Override
    protected WebClient.Builder enrichBuilder(WebClient.Builder builder) {
        return super.enrichBuilder(builder)
                .defaultHeader(PN_NATIONAL_REGISTRIES_CX_ID, "pn-delivery-push");
    }
}
