package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.RefusalCostMode;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Getter
@Component
public class PnTechnicalRefusalCostMode {
    private static final String ERROR_MESSAGE = "Invalid configuration for PN_DELIVERYPUSH_TECHNICAL_REFUSAL_COST_MODE";
    private static final String ERROR_CODE = "INVALID_TECHNICAL_REFUSAL_COST_CONFIGURATION";
    private static final String SEPARATOR = ";";
    private static final int INDEX_REFUSAL_COST_MODE = 0;
    private static final int INDEX_REFUSAL_COST = 1;
    private final RefusalCostMode mode;
    private final Integer cost;

    public PnTechnicalRefusalCostMode(PnDeliveryPushConfigs configs, NotificationProcessCostService notificationProcessCostService) {
        // Retrieve technical refusal cost mode from the configuration property "PN_DELIVERYPUSH_TECHNICAL_REFUSAL_COST_MODE"
        String technicalRefusalCostMode = configs.getTechnicalRefusalCostMode();

        // Configuration property example: PN_DELIVERYPUSH_TECHNICAL_REFUSAL_COST_MODE=RECIPIENT_BASED;50
        if (StringUtils.isNotBlank(technicalRefusalCostMode)) {
            String[] values = technicalRefusalCostMode.split(SEPARATOR);

            if (values.length != 2) {
                throw new PnInternalException(ERROR_MESSAGE, ERROR_CODE);
            }

            this.mode = RefusalCostMode.valueOf(values[INDEX_REFUSAL_COST_MODE]);
            this.cost = Integer.parseInt(values[INDEX_REFUSAL_COST]);
            log.info(String.format("Technical refusal cost mode: %s and refusal cost: %s", mode, cost));

        } else {
            this.mode = RefusalCostMode.RECIPIENT_BASED;
            this.cost = notificationProcessCostService.getSendFee();
            log.info(String.format("Default case - Technical refusal cost mode: %s and refusal cost: %s", mode, cost));
        }
    }

}
