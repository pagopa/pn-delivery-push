package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_DELIVERIES;
import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_IUN;

public class DigitalNotificationWorkFlowHtmlSanitizer extends HtmlSanitizer{

    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        String trustedIun = sanitize((String) templateModelMap.get(FIELD_IUN));
        List<LegalFactGenerator.PecDeliveryInfo> deliveryInfos = (List<LegalFactGenerator.PecDeliveryInfo>) templateModelMap.get(FIELD_DELIVERIES);
        List<LegalFactGenerator.PecDeliveryInfo> trustedPecDeliveryInfos;

        if(CollectionUtils.isEmpty(deliveryInfos)) {
            trustedPecDeliveryInfos = deliveryInfos;
        }
        else {
            trustedPecDeliveryInfos = deliveryInfos.stream().map(this::sanitize).collect(Collectors.toList());
        }

        templateModelMap.put(FIELD_IUN, trustedIun);
        templateModelMap.put(FIELD_DELIVERIES, trustedPecDeliveryInfos);
        return templateModelMap;
    }

    private LegalFactGenerator.PecDeliveryInfo sanitize(LegalFactGenerator.PecDeliveryInfo pecDeliveryInfo) {
        String trustedAddress = sanitize(pecDeliveryInfo.getAddress());
        String trustedDenomination = sanitize(pecDeliveryInfo.getDenomination());
        String trustedTaxId = sanitize(pecDeliveryInfo.getTaxId());
        String trustedResponseDate = sanitize(pecDeliveryInfo.getResponseDate());

        return new LegalFactGenerator.PecDeliveryInfo(trustedDenomination, trustedTaxId, trustedAddress, pecDeliveryInfo.getOrderBy(),
                trustedResponseDate, pecDeliveryInfo.isOk());
    }
}
