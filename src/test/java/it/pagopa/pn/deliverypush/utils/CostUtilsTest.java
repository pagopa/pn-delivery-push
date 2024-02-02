package it.pagopa.pn.deliverypush.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}