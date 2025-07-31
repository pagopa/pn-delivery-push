package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.PaperStatusApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaperTrackerClientImplTest {

    private PaperStatusApi paperStatusApi;

    private PaperTrackerClientImpl paperTrackerClient;

    @BeforeEach
    void setup() {
        paperStatusApi = Mockito.mock(PaperStatusApi.class);
        paperTrackerClient = new PaperTrackerClientImpl(paperStatusApi);
    }

    @Test
    void getPaperStatus() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";
        Mockito.when(paperStatusApi.paperStatus(prepareAnalogDomicileTimelineId, null))
                .thenReturn(new PaperTrackerStatusResponse());

        PaperTrackerStatusResponse response = paperTrackerClient.getPaperStatus(prepareAnalogDomicileTimelineId);

        Assertions.assertNotNull(response);
    }

}