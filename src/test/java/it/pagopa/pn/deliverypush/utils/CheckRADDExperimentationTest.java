package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
class CheckRADDExperimentationTest {
    @Mock
    private ParameterConsumer parameterConsumer;

    private CheckRADDExperimentation checker;


    @BeforeEach
    void setup() {
        checker = new CheckRADDExperimentation(parameterConsumer);
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
        String[] zip = {"2","3","4"};

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia")
                .zip("2").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertTrue(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithZipNotInStore() {
        String[] zip = {"2","3","4"};

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia")
                .zip("21").build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithEmptyZip() {
        String[] zip = {"2","3","4"};

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(zip));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia")
                .build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void foundZipInThirdStore() {
        String[] zip1 = {"2","3","4"};
        String[] zip3 = {"21","22","22"};


        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]), Mockito.any())).thenReturn(Optional.of(zip1));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]), Mockito.any())).thenReturn(Optional.of(zip3));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("22")
                .build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertTrue(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void notFoundZipInStores() {
        String[] zip1 = {"2","3","4"};
        String[] zip2= {};
        String[] zip3 = {"12","13","14"};
        String[] zip4 = {};
        String[] zip5 = {"21","22","22"};


        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[0]), Mockito.any())).thenReturn(Optional.of(zip1));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[1]), Mockito.any())).thenReturn(Optional.of(zip2));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[2]), Mockito.any())).thenReturn(Optional.of(zip3));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[3]), Mockito.any())).thenReturn(Optional.of(zip4));
        Mockito.when(parameterConsumer.getParameterValue(Mockito.eq(CheckRADDExperimentation.PARAMETER_STORES_MAP_ZIP_EXPERIMENTATION_LIST[4]), Mockito.any())).thenReturn(Optional.of(zip5));

        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("224")
                .build();
        boolean isEnabled = checker.checkAddress(addressToCheck);
        Assertions.assertFalse(isEnabled);
    }


}
