package it.pagopa.pn.deliverypush.utils;

public class CostUtils {
    private CostUtils(){}
    
    public static Integer getCostWithVat(Integer vat, Integer cost) {
        Integer costWithVat = null;
        if(vat != null){
            double doubleVat = vat.doubleValue();
            double doubleCost = cost.doubleValue();
            double completeCostWithVat = doubleCost + (doubleCost * doubleVat / 100);
            costWithVat = Math.toIntExact(Math.round(completeCostWithVat));
        }
        return costWithVat;
    }
}
