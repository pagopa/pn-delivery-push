package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Slf4j
@Component
public class CheckRADDExperimentation {
    private static final String[] EXPERIMENTAL_COUNTRIES = {"it", "italia", "italy"};


    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final ParameterConsumer parameterConsumer;

    public CheckRADDExperimentation(ParameterConsumer parameterConsumer, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.parameterConsumer = parameterConsumer;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    private boolean isAnExperimentalCountry(final String countryToCheck) {
        if (StringUtils.isBlank(countryToCheck)) return true;

        for (String currentCountry : CheckRADDExperimentation.EXPERIMENTAL_COUNTRIES) {
            if (StringUtils.equalsIgnoreCase(currentCountry, countryToCheck)) return true;
        }
        return false;
    }

    public boolean checkAddress(PhysicalAddressInt toCheck) {

        if (isAnExperimentalCountry(toCheck.getForeignState())) {
            // country in admitted countries
            List<String> storeNames = pnDeliveryPushConfigs.getRaddExperimentationStoresName();
            if (storeNames == null) return false;
            for (String currentStore : storeNames) {
                log.info("Current Store {}", currentStore);
                if (isInStore(toCheck.getZip(), currentStore)) return true;
            }
        }
        return false;
    }

    private boolean isInStore(String zipCode, String storeName) {
        log.debug("Looking for zip code={}", zipCode);
        Optional<String[]> zipLists = parameterConsumer.getParameterValue(storeName, String[].class);
        if (zipLists.isPresent()) {
            String[] experimentalZipList = zipLists.get();
            for (String currentZip : experimentalZipList) {
                if (currentZip.equals(zipCode)) {
                    log.debug("zipCode={} is in experimental list", zipCode);
                    return true;
                }
            }
        }
        log.debug("zipCode={} not found in experimental list", zipCode);
        return false;
    }
}
