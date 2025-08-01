package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.PaperTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperTrackerServiceImpl implements PaperTrackerService {
    private final PaperTrackerClient paperTrackerClient;
    @Override
    public boolean isPresentDematForPrepareRequest(String prepareAnalogDomicileTimelineId) {
        log.info("Invoking PaperTrackerClient to check paper status for prepareAnalogDomicileTimelineId: {}", prepareAnalogDomicileTimelineId);
        PaperTrackerStatusResponse response = paperTrackerClient.getPaperStatus(prepareAnalogDomicileTimelineId);
        return Boolean.TRUE.equals(response.getFinalDemaStatusFound()) || Boolean.TRUE.equals(response.getFinalStatusFound());
    }
}
