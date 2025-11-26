package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClientReactive pnDataVaultClientReactive;
    
    public ConfidentialInformationServiceImpl(PnDataVaultClientReactive pnDataVaultClientReactive) {
        this.pnDataVaultClientReactive = pnDataVaultClientReactive;
    }

    @Override
    public Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId) {
        return pnDataVaultClientReactive.getRecipientsDenominationByInternalId(List.of(internalId))
                .filter( el -> internalId.equals(el.getInternalId()))
                .map( el -> BaseRecipientDtoInt.builder()
                        .taxId(el.getTaxId())
                        .denomination(el.getDenomination())
                        .internalId(el.getInternalId())
                        .recipientType(el.getRecipientType() != null ? RecipientTypeInt.valueOf(el.getRecipientType().getValue()) : null)
                        .internalId(el.getInternalId())
                        .build()
                ).collectList()
                .map(list -> {
                    if(list != null && !list.isEmpty())
                        return list.get(0);
                    else 
                        return null;
                });
    }

}
