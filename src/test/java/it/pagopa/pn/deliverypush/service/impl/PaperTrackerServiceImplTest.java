package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.PaperTrackerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaperTrackerServiceImplTest {
    private PaperTrackerClient paperTrackerClient;
    private PaperTrackerService paperTrackerService;

    @BeforeEach
    public void setUp() {
        paperTrackerClient = Mockito.mock(PaperTrackerClient.class);
        paperTrackerService = new PaperTrackerServiceImpl(paperTrackerClient);
    }

    @Test
    void isPresentDematForPrepareRequestIsTrueForFinalDemat() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        Mockito.when(paperTrackerClient.getPaperStatus(prepareAnalogDomicileTimelineId))
                .thenReturn(new PaperTrackerStatusResponse()
                        .finalDemaStatusFound(true));

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getPaperStatus(prepareAnalogDomicileTimelineId);
        Assertions.assertTrue(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsTrueForFinalStatus() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        Mockito.when(paperTrackerClient.getPaperStatus(prepareAnalogDomicileTimelineId))
                .thenReturn(new PaperTrackerStatusResponse()
                        .finalStatusFound(true));

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getPaperStatus(prepareAnalogDomicileTimelineId);
        Assertions.assertTrue(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalse() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        Mockito.when(paperTrackerClient.getPaperStatus(prepareAnalogDomicileTimelineId))
                .thenReturn(new PaperTrackerStatusResponse());

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getPaperStatus(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

}