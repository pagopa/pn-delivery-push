package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.exceptions.PnRootIdNonFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PnExternalRegistryClientImplTest {

    private RootSenderIdApi rootSenderIdApi;

    private PnExternalRegistryClientImpl client;

    @BeforeEach
    void setup() {
        rootSenderIdApi = Mockito.mock(RootSenderIdApi.class);
        client = new PnExternalRegistryClientImpl(rootSenderIdApi);
    }


    @Test
    void getRootSenderIdReturnsRootIdWhenApiReturnsResponse() {
        String senderId = "AOO123";
        String expectedRootId = "ROOT456";
        it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.RootSenderIdResponse response =
                org.mockito.Mockito.mock(it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.RootSenderIdResponse.class);

        Mockito.when(rootSenderIdApi.getRootSenderIdPrivate(senderId)).thenReturn(response);
        Mockito.when(response.getRootId()).thenReturn(expectedRootId);

        String actualRootId = client.getRootSenderId(senderId);

        org.junit.jupiter.api.Assertions.assertEquals(expectedRootId, actualRootId);
    }

    @org.junit.jupiter.api.Test
    void getRootSenderIdThrowsPnRootIdNonFoundExceptionWhenApiThrowsException() {
        String senderId = "AOO789";
        Mockito.when(rootSenderIdApi.getRootSenderIdPrivate(senderId))
                .thenThrow(new RuntimeException("API error"));

        Assertions.assertThrows(
                PnRootIdNonFoundException.class,
                () -> client.getRootSenderId(senderId)
        );
    }

    @Test
    void getRootSenderIdThrowsPnRootIdNonFoundExceptionWhenResponseIsNull() {
        String senderId = "AOO000";
        Mockito.when(rootSenderIdApi.getRootSenderIdPrivate(senderId)).thenReturn(null);

        Assertions.assertThrows(
                PnRootIdNonFoundException.class,
                () -> client.getRootSenderId(senderId)
        );
    }
}