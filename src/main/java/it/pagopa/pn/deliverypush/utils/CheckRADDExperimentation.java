
package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Component

@Slf4j
public class CheckRADDExperimentation {
    private static final String[] EXPERIMENTAL_COUNTRIES = {"it", "italia", "italy"};
    public static final String[] PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST = {"radd-experimentation-zip-1",
            "radd-experimentation-zip-2", "radd-experimentation-zip-3",
            "radd-experimentation-zip-4", "radd-experimentation-zip-5"};
    private final ParameterConsumer parameterConsumer;

    public CheckRADDExperimentation(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    private static boolean isAnExperimentalCountry(final String countryToCheck) {
        if (StringUtils.isBlank(countryToCheck)) return true;

        for (String currentCountry : CheckRADDExperimentation.EXPERIMENTAL_COUNTRIES) {
            if (StringUtils.equalsIgnoreCase(currentCountry, countryToCheck)) return true;
        }
        return false;
    }

    public boolean checkAddress(PhysicalAddressInt toCheck) {

        if (CheckRADDExperimentation.isAnExperimentalCountry(toCheck.getForeignState())) {
            // country in admitted countries
            for (String currentStore : CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST) {
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
