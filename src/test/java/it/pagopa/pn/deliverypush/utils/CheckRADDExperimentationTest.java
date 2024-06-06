package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

class CheckRADDExperimentationTest {
    private final static String[] PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST = {"radd-expeAAArimentation-zip-1", "radd-experimentation-zip-2", "radd-experimentation-zip-3", "radd-experimentation-zip-4", "radd-experimentation-zip-5"};
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs = pnDeliveryPushConfigs();
    @Mock
    private ParameterConsumer parameterConsumer;
    private CheckRADDExperimentation checker;

    public PnDeliveryPushConfigs pnDeliveryPushConfigs() {
        PnDeliveryPushConfigs pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);

        // Base configuration
        List<String> pnRaddExperimentationStore = new ArrayList<>();
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[3]);
        pnRaddExperimentationStore.add(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[4]);
        Mockito.when(pnDeliveryPushConfigs.getRaddExperimentationStoresName()).thenReturn(pnRaddExperimentationStore);

        return pnDeliveryPushConfigs;
    }

    @BeforeEach
    void setup() {
        checker = new CheckRADDExperimentation(parameterConsumer, pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithNoState() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState(null).build();
        // addressToCheck.set
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithForeignState() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("US").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithZipInStore() {

        final String[] SET_VALUES = new String[]{"2", "3", "4"};
        final Set<String> zip = new HashSet<>(Arrays.asList(SET_VALUES));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("2").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertTrue(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithZipNotInStore() {
        final String[] SET_VALUES = new String[]{"2", "3", "4"};
        final Set<String> zip = new HashSet<>(Arrays.asList(SET_VALUES));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("21").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithEmptyZip() {
        final String[] SET_VALUES = new String[]{"2", "3", "4"};
        final Set<String> zip = new HashSet<>(Arrays.asList(SET_VALUES));

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void foundZipInThirdStore() {
        final String[] SET_VALUES = new String[]{"2", "3", "4"};
        final Set<String> zip1 = new HashSet<>(Arrays.asList(SET_VALUES));
        final String[] SET_VALUES1 = new String[]{"21", "22"};
        final Set<String> zip2 = new HashSet<>(Arrays.asList(SET_VALUES1));

        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]), Mockito.any())).thenReturn(Optional.of(zip1));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]), Mockito.any())).thenReturn(Optional.of(zip2));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("22").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertTrue(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void notFoundZipInStores() {

        final String[] SET_VALUES = new String[]{"2", "3", "4"};
        final String[] SET_VALUES1 = new String[]{};
        final String[] SET_VALUES2 = new String[]{"12", "13", "14"};
        final String[] SET_VALUES3 = new String[]{};
        final String[] SET_VALUES4 = new String[]{"21", "22", "22"};

        final Set<String> zip1 = new HashSet<>(Arrays.asList(SET_VALUES));
        final Set<String> zip2 = new HashSet<>(Arrays.asList(SET_VALUES1));
        final Set<String> zip3 = new HashSet<>(Arrays.asList(SET_VALUES2));
        final Set<String> zip4 = new HashSet<>(Arrays.asList(SET_VALUES3));
        final Set<String> zip5 = new HashSet<>(Arrays.asList(SET_VALUES4));

        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]), Mockito.any())).thenReturn(Optional.of(zip1));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]), Mockito.any())).thenReturn(Optional.of(zip2));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]), Mockito.any())).thenReturn(Optional.of(zip3));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[3]), Mockito.any())).thenReturn(Optional.of(zip4));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[4]), Mockito.any())).thenReturn(Optional.of(zip5));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("224").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }


}
