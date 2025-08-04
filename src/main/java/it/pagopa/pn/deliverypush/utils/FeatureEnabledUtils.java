package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@AllArgsConstructor
@Component
public class FeatureEnabledUtils {

    private final PnDeliveryPushConfigs configs;

    public boolean isPfNewWorkflowEnabled(Instant notificationSentAt) {
        boolean isEnabled = false;
        Instant startDate = Instant.parse(configs.getPfNewWorkflowStart());
        Instant endDate = Instant.parse(configs.getPfNewWorkflowStop());
        if (notificationSentAt.compareTo(startDate) >= 0 && notificationSentAt.compareTo(endDate) <= 0) {
            isEnabled = true;
        }
        return isEnabled;
    }

    public boolean isFeatureAAROnlyPECForRADDAndPFEnabled(){
        return Optional.ofNullable(configs.getAAROnlyPECForRADDAndPF())
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

    public boolean isAnalogWorkflowTimeoutFeatureEnabled(Instant notificationSentAt) {
        Instant startDate = configs.getStartAnalogWorkflowTimeoutFeatureDate();
        return startDate != null && notificationSentAt.compareTo(startDate) >= 0;
    }

}
