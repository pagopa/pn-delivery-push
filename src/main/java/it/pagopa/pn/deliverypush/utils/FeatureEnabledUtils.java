package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@AllArgsConstructor
@Component
public class FeatureEnabledUtils {

    private final PnDeliveryPushConfigs configs;

    public boolean isPerformanceImprovementEnabled(Instant notBefore) {
        boolean isEnabled = false;
        Instant startDate = Instant.parse(configs.getPerformanceImprovementStartDate());
        Instant endDate = Instant.parse(configs.getPerformanceImprovementEndDate());
        if ( notBefore.compareTo(startDate) >= 0 && notBefore.compareTo(endDate) <= 0) {
            isEnabled = true;
        }
        return isEnabled;
    }

}
