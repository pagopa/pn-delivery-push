package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.rest.PnDeliveryPushRestApi_methodProcessAction;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PnScheduleActionController implements PnDeliveryPushRestApi_methodProcessAction {
    
    @Override
    @GetMapping(PnDeliveryPushRestConstants.PROCESS_ACTION)
    public void processAction() {
       log.info("START PROCESS ACTION !!!");
    }

}
