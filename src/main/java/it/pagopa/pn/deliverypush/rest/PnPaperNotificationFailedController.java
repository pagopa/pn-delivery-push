package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PnPaperNotificationFailedController {

    private final PaperNotificationFailedService service;

    public PnPaperNotificationFailedController(PaperNotificationFailedService service) {
        this.service = service;
    }

    @GetMapping(PnDeliveryPushRestConstants.NOTIFICATIONS_PAPER_FAILED_PATH)
    public ResponseEntity<List<PaperNotificationFailed>> searchPaperNotificationsFailed(
            @RequestParam(name = "recipientId") String recipientId) {
        return ResponseEntity.ok().body(service.getPaperNotificationByRecipientId(recipientId));
    }

}
