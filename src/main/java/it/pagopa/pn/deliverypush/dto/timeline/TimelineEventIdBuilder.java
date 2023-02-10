package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Classe builder che permette di costruire un timelineEventId
 *
 * Il formato dello della stringa di input dovrà essere:
 * <CATEGORY_VALUE>-IUN_<IUN_VALUE>-RECINDEX_<RECINDEX_VALUE>...
 * tutti i value sono facoltativi, tranne il campo category.
 * Sarà responsabilità del builder concatenare ogni singolo value alla timelineEventId solo se non gli viene passato null.
 */
public class TimelineEventIdBuilder {

    private String iun = "";

    private String recIndex = "";

    private String category = "";

    private String source = "";

    private String sentAttemptMade = "";

    private String progressIndex = "";

    private String deliveryMode = "";

    private String contactPhase = "";

    private String correlationId = ""; // for national registries

    private String courtesyAddressType = "";

    public TimelineEventIdBuilder withIun(@Nullable String iun) {
        if(iun != null)
            this.iun = "-IUN_".concat(iun);
        return this;
    }

    public TimelineEventIdBuilder withRecIndex(@Nullable Integer recIndex) {
        if(recIndex != null)
            this.recIndex = "-RECINDEX_".concat(recIndex + "");
        return this;
    }

    public TimelineEventIdBuilder withCategory(@NotNull String category) {
        this.category = category;
        return this;
    }

    public TimelineEventIdBuilder withSource(@Nullable DigitalAddressSourceInt source) {
        if(source != null)
            this.source = "-SOURCE_".concat(source.getValue());
        return this;
    }

    public TimelineEventIdBuilder withSentAttemptMade(@Nullable Integer sentAttemptMade) {
        if(sentAttemptMade != null && sentAttemptMade >= 0)
            this.sentAttemptMade = "-SENTATTEMPTMADE_".concat(sentAttemptMade + "");
        return this;
    }

    public TimelineEventIdBuilder withProgressIndex(@Nullable Integer progressIndex) {
        // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
        if(progressIndex != null && progressIndex >= 0)
            this.progressIndex = "-PROGRESSINDEX_".concat(progressIndex + "");
        return this;
    }

    public TimelineEventIdBuilder withDeliveryMode(@Nullable DeliveryModeInt deliveryMode) {
        if(deliveryMode != null)
            this.deliveryMode = "-DELIVERYMODE_".concat(deliveryMode.getValue());
        return this;
    }

    public TimelineEventIdBuilder withContactPhase(@Nullable ContactPhaseInt contactPhase) {
        if(contactPhase != null)
            this.contactPhase = "-CONTACTPHASE_".concat(contactPhase.getValue());
        return this;
    }

    public TimelineEventIdBuilder withCorrelationId(@Nullable String correlationId) {
        if(correlationId != null)
            this.correlationId = "-CORRELATIONID_".concat(correlationId);
        return this;
    }

    public TimelineEventIdBuilder withCourtesyAddressType(@Nullable CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyAddressType) {
        if(courtesyAddressType != null)
            this.courtesyAddressType = "-COURTESYADDRESSTYPE_".concat(courtesyAddressType.getValue());
        return this;
    }

    public String build() {
        return new StringBuilder()
                .append(category)
                .append(iun)
                .append(recIndex)
                .append(courtesyAddressType)
                .append(source)
                .append(deliveryMode)
                .append(contactPhase)
                .append(sentAttemptMade)
                .append(progressIndex)
                .append(correlationId)
                .toString();
    }

    public String buildFromEventId(TimelineEventId timelineEventId, EventId eventId) {
        return new TimelineEventIdBuilder()
                .withCategory(timelineEventId.getValue())
                .withIun(eventId.getIun())
                .withRecIndex(eventId.getRecIndex())
                .withSource(eventId.getSource())
                .withSentAttemptMade(eventId.getSentAttemptMade())
                .withProgressIndex(eventId.getProgressIndex())
                .withDeliveryMode(eventId.getDeliveryMode())
                .withContactPhase(eventId.getContactPhase())
                .withCourtesyAddressType(eventId.getCourtesyAddressType())
                .build();
    }

    public String buildFromCorrelationId(TimelineEventId timelineEventId, String correlationId) {
        return new TimelineEventIdBuilder()
                .withCategory(timelineEventId.getValue())
                .withCorrelationId(correlationId)
                .build();
    }

}
