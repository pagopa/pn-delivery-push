package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AnalogAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeResult;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;

@FunctionalSpringBootTest
@Import(LocalStackTestConfig.class)
class AddressManagerEventHandlerTestIT {

    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private AddressManagerResponseHandler handler;

    @Test
    void consumeMessageOK() {
        Consumer<Message<NormalizeItemsResult>> pnNationalRegistriesEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnAddressManagerEventInboundConsumer");
        
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult()
                .correlationId("VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1")
                .addResultItemsItem(new NormalizeResult().normalizedAddress(
                        new AnalogAddress()
                                .addressRow("Info1")
                                .addressRow2("Info2")
                                .cap("80078")
                                .pr("NA")
                                .city("Lago")
                                .city2("Nuovo")
                                .country("IT")
                ));
        
        Message<NormalizeItemsResult> message = MessageBuilder.withPayload(normalizeItemsResult).build();
        pnNationalRegistriesEventInboundConsumer.accept(message);
        Mockito.verify(handler).handleResponseReceived(Mockito.any());
    }


}
