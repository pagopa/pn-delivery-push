package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class CourtesyMessagesReport {
    private final List<CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT> sentCourtesyTypes = new ArrayList<>();
    private final List<CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT> notSentCourtesyTypes = new ArrayList<>();
    private final List<CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT> courtesyTypesInError = new ArrayList<>();
    @Setter
    private Instant schedulingAnalogDate;

    public boolean hasSentAtLeastACourtesyMessage() {
        return !sentCourtesyTypes.isEmpty();
    }

    public void addSentCourtesyType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyType) {
        log.debug("Adding sent courtesy type: {}", courtesyType);
        this.sentCourtesyTypes.add(courtesyType);
    }

    public void addNotSentCourtesyType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyType) {
        log.debug("Adding not sent courtesy type: {}", courtesyType);
        this.notSentCourtesyTypes.add(courtesyType);
    }

    public void addCourtesyTypeInError(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyType) {
        log.debug("Adding courtesy type in error: {}", courtesyType);
        this.courtesyTypesInError.add(courtesyType);
    }
}

