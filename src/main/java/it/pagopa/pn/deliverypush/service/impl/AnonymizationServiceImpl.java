package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.service.AnonymizationService;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class AnonymizationServiceImpl implements AnonymizationService {
    private final PnDataVaultClient pnDataVaultClient;

    public AnonymizationServiceImpl(PnDataVaultClient pnDataVaultClient) {
        this.pnDataVaultClient = pnDataVaultClient;
    }


    @Override
    public void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDtoInt confidentialTimelineElementDto) {
        //ResponseEntity<Void> response = pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(iun)
    }

    @Override
    public ConfidentialTimelineElementDtoInt getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId) {
        return null;
    }

    @Override
    public ResponseEntity<List<BaseRecipientDtoInt>> getRecipientDenominationByInternalId(List<String> internalId) {
        return null;
    }
}
