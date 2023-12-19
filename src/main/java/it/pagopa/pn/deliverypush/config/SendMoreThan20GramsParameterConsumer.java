package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 *
 * quickWorkAroundForPN-9116
 */
@Slf4j
@Component
public class SendMoreThan20GramsParameterConsumer {


    private final ParameterConsumer parameterConsumer;

    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private static final String PARAMETER_STORE_MAP_PA_SEND_MORE = "MapPaSendMoreThan20Grams";


    public SendMoreThan20GramsParameterConsumer(ParameterConsumer parameterConsumer, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.parameterConsumer = parameterConsumer;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public Boolean isPaEnabledToSendMoreThan20Grams(String paTaxId) {
        log.debug( "Start isPaEnabledToSendMoreThan20Grams for paTaxId={}", paTaxId );

        Optional<PaTaxIdCanSendMoreThan20Grams[]> optionalPaTaxIdCanSendMoreThan20Grams = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MAP_PA_SEND_MORE, PaTaxIdCanSendMoreThan20Grams[].class);
        if( optionalPaTaxIdCanSendMoreThan20Grams.isPresent() ) {
            PaTaxIdCanSendMoreThan20Grams[] paTaxIdCanSendMoreThan20Grams = optionalPaTaxIdCanSendMoreThan20Grams.get();
            for (PaTaxIdCanSendMoreThan20Grams paTaxIdcanSendMoreThan20Grams : paTaxIdCanSendMoreThan20Grams ) {
                if ( paTaxIdcanSendMoreThan20Grams.paTaxId.equals(paTaxId) ) {
                    Boolean canSendMoreThan20Grams = paTaxIdcanSendMoreThan20Grams.canSendMoreThan20Grams;
                    log.debug("paTaxId={} canSendMoreThan20Grams={}", paTaxId, canSendMoreThan20Grams);
                    return canSendMoreThan20Grams;
                }
            }
        }

        log.debug("paTaxId={} configuration not found, sendMoreThan20GramsDefaultValue={}", paTaxId, pnDeliveryPushConfigs.isSendMoreThan20GramsDefaultValue());
        return pnDeliveryPushConfigs.isSendMoreThan20GramsDefaultValue();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class PaTaxIdCanSendMoreThan20Grams {
        String paTaxId;
        Boolean canSendMoreThan20Grams;
    }
}
