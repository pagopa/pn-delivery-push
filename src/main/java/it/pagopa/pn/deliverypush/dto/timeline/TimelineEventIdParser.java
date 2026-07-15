package it.pagopa.pn.deliverypush.dto.timeline;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser per timeline event IDs costruiti con TimelineEventIdBuilder.
 *
 * Formato: CATEGORY.IUN_{value}.RECINDEX_{value}.SOURCE_{value}...
 *
 * Esempio uso:
 * <pre>
 * var parser = TimelineEventIdParser.parse(eventId);
 * Integer recIndex = parser.recIndex().orElse(null);
 * String iun = parser.iun().orElse(null);
 *
 * // oppure ottieni tutti i componenti
 * var components = parser.toComponents();
 * if (components.hasRecIndex()) { ... }
 * </pre>
 */
public class TimelineEventIdParser {

    private static final Pattern IUN_PATTERN = Pattern.compile("IUN_([^.]+)");
    private static final Pattern RECINDEX_PATTERN = Pattern.compile("RECINDEX_(\\d+)");
    private static final Pattern ATTEMPT_PATTERN = Pattern.compile("ATTEMPT_(\\d+)");
    private static final Pattern REWORK_PATTERN = Pattern.compile("REWORK_(\\d+)");

    private final String eventId;

    private TimelineEventIdParser(String eventId) {
        this.eventId = eventId != null ? eventId : "";
    }

    /**
     * Crea un parser per l'eventId specificato
     */
    public static TimelineEventIdParser parse(String eventId) {
        return new TimelineEventIdParser(eventId);
    }

    /**
     * Estrae la categoria (primo elemento prima del punto)
     */
    public Optional<String> category() {
        if (eventId.isEmpty()) return Optional.empty();
        int firstDot = eventId.indexOf('.');
        return Optional.of(firstDot > 0 ? eventId.substring(0, firstDot) : eventId);
    }

    /**
     * Estrae l'IUN
     */
    public Optional<String> iun() {
        return extract(IUN_PATTERN);
    }

    /**
     * Estrae il recipient index
     */
    public Optional<Integer> recIndex() {
        return extract(RECINDEX_PATTERN).map(Integer::parseInt);
    }

    /**
     * Estrae il numero di tentativo
     */
    public Optional<Integer> sentAttemptMade() {
        return extract(ATTEMPT_PATTERN).map(Integer::parseInt);
    }

    /**
     * Estrae il rework index
     */
    public Optional<Integer> reworkIndex() {
        return extract(REWORK_PATTERN).map(Integer::parseInt);
    }

    public Optional<String> reworkIndexFull() {
        return extract(REWORK_PATTERN).flatMap(idx -> {
            Matcher matcher = REWORK_PATTERN.matcher(eventId);
            return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
        });
    }

    /**
     * Converte tutti i componenti in un record
     */
    public TimelineEventIdComponents toComponents() {
        return new TimelineEventIdComponents(
                category().orElse(null),
                iun().orElse(null),
                recIndex().orElse(null),
                sentAttemptMade().orElse(null),
                reworkIndex().orElse(null)
        );
    }

    private Optional<String> extract(Pattern pattern) {
        if (eventId.isEmpty()) return Optional.empty();

        Matcher matcher = pattern.matcher(eventId);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /**
     * Record contenente tutti i componenti estratti dall'eventId
     */
    public record TimelineEventIdComponents(
            String category,
            String iun,
            Integer recIndex,
            Integer sentAttemptMade,
            Integer reworkIndex
    ) {
        public boolean hasCategory() { return category != null; }
        public boolean hasIun() { return iun != null; }
        public boolean hasRecIndex() { return recIndex != null; }
        public boolean hasSentAttemptMade() { return sentAttemptMade != null; }
        public boolean hasReworkIndex() { return reworkIndex != null; }
    }
}