package it.pagopa.pn.deliverypush.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CostUtilsTest {
    @Test
    void getCostWithVat() {
        //vat 22
        getCostWithVatAndCheck(22, 1000, 1220);
        //vat 0
        getCostWithVatAndCheck(0, 1000, 1000);
        // RoundingDown
        getCostWithVatAndCheck(22, 969, 1182);
        // RoundingUp
        getCostWithVatAndCheck(22, 971, 1185);
    }
    
    private void getCostWithVatAndCheck(Integer vat, Integer cost, Integer expectedResult){
        Integer costWithVat = CostUtils.getCostWithVat(vat, cost);
        Assertions.assertEquals(expectedResult, costWithVat);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "22, 436, 532", // Round Up Test
            "22, 397, 484", // Round Down Test
            "22, 1000, 1220", // 22% VAT
            "10, 1500, 1650", // 10% VAT
            "0, 1500, 1500", // Test with 0% VAT
            "NULL, 1500, NULL" // Test with null VAT
    }, nullValues = {"NULL"})
    void getCostWithVatTest(Integer vat, Integer cost, Integer expectedCostWithVat) {

        Integer costWithVat = CostUtils.getCostWithVat(vat, cost);

        Assertions.assertEquals(expectedCostWithVat, costWithVat);
    }
}