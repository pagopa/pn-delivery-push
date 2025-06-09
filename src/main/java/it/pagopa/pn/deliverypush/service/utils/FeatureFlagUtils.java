package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class FeatureFlagUtils {
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private static final String MISSING_PARAMS_ERROR_MESSAGE = "The parameters to choose Action Implementation are null or not configured, startDate = %s endDate = %s.";
    private static final String MISSING_PARAMS_ERROR_CODE = "PN_DELIVERY_PUSH_MISSING_ACTION_IMPL_FEATURE_FLAG";

    public boolean isActionImplDao(Instant now) {
        Instant startDate = pnDeliveryPushConfigs.getStartActionImplWithDaoTimestamp();
        Instant endDate = pnDeliveryPushConfigs.getEndActionImplWithDaoTimestamp();
        if(Objects.isNull(startDate) || Objects.isNull(endDate)) {
            String message = String.format(MISSING_PARAMS_ERROR_MESSAGE, startDate, endDate);
            throw new PnInternalException(message, MISSING_PARAMS_ERROR_CODE);
        }

        return startDate.compareTo(now) <= 0 && endDate.compareTo(now) > 0;
    }
}
