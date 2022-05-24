package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodSearchPaperNotificationFailed;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PnPaperNotificationFailedController implements PnDeliveryRestApi_methodSearchPaperNotificationFailed {

    private final PaperNotificationFailedService service;

    public PnPaperNotificationFailedController(PaperNotificationFailedService service) {
        this.service = service;
    }

    @Override
    @GetMapping(PnDeliveryPushRestConstants.NOTIFICATIONS_PAPER_FAILED_PATH)
    public ResponseEntity<List<PaperNotificationFailed>> searchPaperNotificationsFailed(
            @RequestParam(name = "recipientId") String recipientId) {
        return ResponseEntity.ok().body(service.getPaperNotificationByRecipientId(recipientId));
    }

}
