package it.pagopa.pn.deliverypush.dto.timeline;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Classe builder che permette di costruire un timelineEventId
 *
 * Il formato dello della stringa di input dovrà essere:
 * <CATEGORY_VALUE>_IUN_<IUN_VALUE>_RECINDEX_<RECINDEX_VALUE>...
 * tutti i value sono facoltativi, tranne il campo category.
 * Sarà responsabilità del builder concatenare ogni singolo value alla timelineEventId solo se non gli viene passato null.
 */
public class TimelineEventIdBuilder {

    private String iun = "";

    private String recIndex = "";

    private String category = "";

    public TimelineEventIdBuilder withIun(@Nullable String iun) {
        if(iun != null)
            this.iun = "IUN_".concat(iun).concat("_");
        return this;
    }

    public TimelineEventIdBuilder withRecIndex(@Nullable Integer recIndex) {
        if(recIndex != null)
            this.recIndex = "RECINDEX".concat(recIndex + "").concat("_");
        return this;
    }

    public TimelineEventIdBuilder withCategory(@NotNull String category) {
        this.category = category.concat("_");
        return this;
    }

    public String build() {
        return new StringBuilder()
                .append(category)
                .append(iun)
                .append(recIndex)
                .toString();
    }

}
