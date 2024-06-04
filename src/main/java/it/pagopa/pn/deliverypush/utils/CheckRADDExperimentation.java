package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


import it.pagopa.pn.commons.abstractions.ParameterConsumer;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CheckRADDExperimentation {
    private static final String[] experimentalCountries = ["it", "italia", "italy"];
    private static final String[] PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST =
        ["RADD_EXPER", "italia", "italy"];

    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_STORE_MAP_TAX_ID_WHITE_LIST_NAME = "MapTaxIdWhiteList";

    public CheckRADDExperimentation(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    public static boolean checkAddress(PhysicalAddressInt toCheck){
        // countTry in admitted countries
        if (isAnExperimentalCountry(toCheck.getForeignState()))

            // TOOD zip in paramater store
            return true;
        //  toCheck.zip
        return true;

    }


    public Boolean isInStore(String zipCode, String storeName ) {
        log.debug( "Looking for zip code={}", zipCode);
        Optional<String[]> zipLists = parameterConsumer.getParameterValue(storeName, String[].class);
        if (zipLists.isPresent()) {
            String[] experimentalZipList = zipLists.get();
            for (String currentZip : experimentalZipList) {
                if ( currentZip.equals( zipCode ) ) {
                    log.debug("zipCode={} is in experimental list", zipCode );
                    return true;
                }
            }
        }
        log.debug("zipCode={} not found in experimental list", zipCode);
        return false;
    }

    private static boolean isAnExperimentalCountry(final String countryToCheck){
        if  (StringUtils.isBlank(countryToCheck)) return true;

        for (String currentCountry: experimentalCountries){
            if (StringUtils.equalsIgnoreCase(currentCountry, countryToCheck))
                return true;
        }
        return false;
    }
}
