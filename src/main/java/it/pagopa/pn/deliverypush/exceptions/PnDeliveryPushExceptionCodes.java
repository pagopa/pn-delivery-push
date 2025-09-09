package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import lombok.Getter;

public class PnDeliveryPushExceptionCodes extends PnExceptionsCodes {

    // raccolgo qui tutti i codici di errore di delivery push
    public static final String ERROR_CODE_DELIVERYPUSH_NOTFOUND = "PN_DELIVERYPUSH_NOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_GETFILEERROR = "PN_DELIVERYPUSH_GETFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR = "PN_DELIVERYPUSH_UPLOADFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR = "PN_DELIVERYPUSH_UPDATEMETAFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_ERRORCOMPUTECHECKSUM = "PN_DELIVERYPUSH_ERRORCOMPUTECHECKSUM";
    public static final String ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED = "PN_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINEEVENTNOTFOUND = "PN_DELIVERYPUSH_TIMELINEEVENTNOTFOUND";
    public static final String ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED = "PN_WEBHOOK_MAXSTREAMSCOUNTREACHED";
    public static final String ERROR_CODE_WEBHOOK_FORBIDDEN = "PN_WEBHOOK_FORBIDDEN";
    public static final String ERROR_CODE_WEBHOOK_NOT_FOUND = "PN_WEBHOOK_NOT_FOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND = "PN_DELIVERYPUSH_STATUSNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_ROOTIDNOTFOUND = "PN_DELIVERYPUSH_ROOTIDNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_AUDITLOGFAILED = "PN_DELIVERYPUSH_AUDITLOGFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_HANDLEEVENTFAILED = "PN_DELIVERYPUSH_HANDLEEVENTFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_NO_RECIPIENT_IN_NOTIFICATION = "PN_DELIVERYPUSH_NORECIPIENTINNOTIFICATION";
    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED = "PN_DELIVERYPUSH_NOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_DUPLICATED_ITEMD = "PN_DELIVERYPUSH_DUPLICATED_ITEM";
    public static final String ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR = "PN_DELIVERYPUSH_READ_FILE_ERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND = "PN_DELIVERYPUSH_FILE_NOT_FOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED = "PN_DELIVERYPUSH_NOTIFICATION_CANCELLED";
    public static final String ERROR_CODE_DELIVERYPUSH_PAYMENT_UPDATE_RETRY_EXCEPTION = "ERROR_CODE_DELIVERYPUSH_PAYMENTUPDATERETRYEXCEPTION";
    public static final String ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND = "ERROR_CODE_DELIVERYPUSH_CONFIGURATIONNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT = "ERROR_CODE_DELIVERYPUSH_TIMELINEELEMENTNOTPRESENT";
    public static final String ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_PRESENT = "ERROR_CODE_DELIVERYPUSH_TOTALCOSTNOTPRESENT";
    public static final String ERROR_CODE_DELIVERYPUSH_FILE_GONE = "PN_DELIVERYPUSH_FILE_GONE";
    public static final String ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED = "PN_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED";


    @Getter
    public enum NotificationRefusedErrorCodeInt {
        RECIPIENT_ID_NOT_VALID("RECIPIENT_ID_NOT_VALID", false),

        ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", false);

        private final String value;

        private final Boolean isTechnicalRefusal;

        NotificationRefusedErrorCodeInt(String value, Boolean isTechnicalRefusal) {
            this.value = value;
            this.isTechnicalRefusal = isTechnicalRefusal;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static NotificationRefusedErrorCodeInt fromValue(String value) {
            for (NotificationRefusedErrorCodeInt b : NotificationRefusedErrorCodeInt.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            return null;
        }
    }
}
