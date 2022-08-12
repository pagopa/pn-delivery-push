package it.pagopa.pn.deliverypush.dto.timeline.details;

public interface TimelineElementDetailsInt {
    /**
     * This method need to return a String with useful timeline details information to be insert in audit log.
     * Sensitive information must NOT be returned (taxId, email address, address number, physical address etc.)
     */
    String toLog();
}
