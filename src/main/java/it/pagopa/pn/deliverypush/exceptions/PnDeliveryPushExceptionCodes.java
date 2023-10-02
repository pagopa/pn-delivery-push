package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;

public class PnDeliveryPushExceptionCodes extends PnExceptionsCodes {

    // raccolgo qui tutti i codici di errore di delivery push
    public static final String ERROR_CODE_DELIVERYPUSH_NOTFOUND = "PN_DELIVERYPUSH_NOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_GETFILEERROR = "PN_DELIVERYPUSH_GETFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_SHAFILEERROR = "PN_DELIVERYPUSH_SHAFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR = "PN_DELIVERYPUSH_UPLOADFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR = "PN_DELIVERYPUSH_UPDATEMETAFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_ERRORCOMPUTECHECKSUM = "PN_DELIVERYPUSH_ERRORCOMPUTECHECKSUM";
    public static final String ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED = "PN_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE = "PN_DELIVERYPUSH_INVALIDEVENTCODE";
    public static final String ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT = "PN_DELIVERYPUSH_INVALIDATTEMPT";
    public static final String ERROR_CODE_DELIVERYPUSH_INVALIDADDRESSSOURCE = "PN_DELIVERYPUSH_INVALIDADDRESSSOURCE";
    public static final String ERROR_CODE_DELIVERYPUSH_SENDDIGITALTIMELINEEVENTNOTFOUND = "PN_DELIVERYPUSH_SENDDIGITALTIMELINEEVENTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_SCHEDULEDDIGITALTIMELINEEVENTNOTFOUND = "PN_DELIVERYPUSH_SCHEDULEDDIGITALTIMELINEEVENTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_SCHEDULEDPREPARETIMELINEEVENTNOTFOUND = "PN_DELIVERYPUSH_SCHEDULEDPREPARETIMELINEEVENTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_LASTADDRESSATTEMPTNOTFOUND = "PN_DELIVERYPUSH_LASTADDRESSATTEMPTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINEEVENTNOTFOUND = "PN_DELIVERYPUSH_TIMELINEEVENTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY = "PN_DELIVERYPUSH_ERRORCOURTESY";
    public static final String ERROR_CODE_DELIVERYPUSH_ERRORCOURTESYIO = "PN_DELIVERYPUSH_ERRORCOURTESYIO";
    public static final String ERROR_CODE_WEBHOOK_UPDATEEVENTSTREAM = "PN_WEBHOOK_UPDATEEVENTSTREAM";
    public static final String ERROR_CODE_WEBHOOK_CONSUMEEVENTSTREAM = "PN_WEBHOOK_CONSUMEEVENTSTREAM";
    public static final String ERROR_CODE_WEBHOOK_SAVEEVENT = "PN_WEBHOOK_SAVEEVENT";
    public static final String ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED = "PN_WEBHOOK_MAXSTREAMSCOUNTREACHED";
    public static final String ERROR_CODE_WEBHOOK_FORBIDDEN = "PN_WEBHOOK_FORBIDDEN";
    public static final String ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS = "PN_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS";
    public static final String ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND = "PN_DELIVERYPUSH_STATUSNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_FEEDBACKNOTFOUND = "PN_DELIVERYPUSH_FEEDBACKNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND = "PN_DELIVERYPUSH_TIMELINENOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONRECIPIENTNOTFOUND = "PN_DELIVERYPUSH_NOTIFICATIONRECIPIENTNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_CONTACTPHASENOTFOUND = "PN_DELIVERYPUSH_CONTACTPHASENOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_DELIVERYNOTFOUND = "PN_DELIVERYPUSH_DELIVERYNOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_UPDATENOTIFICATIONFAILED = "PN_DELIVERYPUSH_UPDATENOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINECONFIDENTIALFAILED = "PN_DELIVERYPUSH_TIMELINECONFIDENTIALFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED = "PN_DELIVERYPUSH_TAXIDNOTICECODEFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_AUDITLOGFAILED = "PN_DELIVERYPUSH_AUDITLOGFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED = "PN_DELIVERYPUSH_SAVELEGALFACTSFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_STATUSUPDATEFAILED = "PN_DELIVERYPUSH_STATUSUPDATEFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED = "PN_DELIVERYPUSH_ADDTIMELINEFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_CHANNELTYPENOTSUPPORTED = "PN_DELIVERYPUSH_CHANNELTYPENOTSUPPORTED";
    public static final String ERROR_CODE_DELIVERYPUSH_SENDPECNOTIFICATIONFAILED = "PN_DELIVERYPUSH_SENDPECNOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_SENDSMSNOTIFICATIONFAILED = "PN_DELIVERYPUSH_SENDSMSNOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_SENDEMAILNOTIFICATIONFAILED = "PN_DELIVERYPUSH_SENDEMAILNOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_ADDRESSTYPENOTSUPPORTED = "PN_DELIVERYPUSH_ADDRESSTYPENOTSUPPORTED";
    public static final String ERROR_CODE_DELIVERYPUSH_HANDLEEVENTFAILED = "PN_DELIVERYPUSH_HANDLEEVENTFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_EVENTTYPENOTSUPPORTED = "PN_DELIVERYPUSH_EVENTTYPENOTSUPPORTED";
    public static final String ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED = "PN_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED";
    public static final String ERROR_CODE_DELIVERYPUSH_ACTIONEXCEPTION = "PN_DELIVERYPUSH_ACTIONEXCEPTION";
    public static final String ERROR_CODE_DELIVERYPUSH_TIMELINEELEMENTFAILED = "PN_DELIVERYPUSH_TIMELINEELEMENTFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_SAVENOTIFICATIONFAILED = "PN_DELIVERYPUSH_SAVENOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONSTATUSFAILED = "PN_DELIVERYPUSH_NOTIFICATION_STATUS_FAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_JSONEXCEPTION = "PN_DELIVERYPUSH_JSONEXCEPTION";
    public static final String ERROR_CODE_WEBHOOK_EVENTFAILED = "PN_WEBHOOK_EVENTFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED = "PN_DELIVERYPUSH_GENERATEPDFFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_UPDATEFAILED = "PN_DELIVERYPUSH_UPDATEFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_NO_RECIPIENT_IN_NOTIFICATION = "PN_DELIVERYPUSH_NORECIPIENTINNOTIFICATION";
    public static final String ERROR_CODE_DELIVERYPUSH_SAVED_LEGALFACT_FAILED = "PN_DELIVERYPUSH_SAVEDLEGALFACTFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_DOCUMENTCOMPOSITIONFAILED = "PN_DELIVERYPUSH_DOCUMENTCOMPOSITIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED = "PN_DELIVERYPUSH_PAPERUPDATEFAILED";

    public static final String ERROR_CODE_DELIVERYPUSH_RECIPIENTS_TOKEN_FAILED = "PN_DELIVERYPUSH_NOTIFICATIONRECIPIENTSTOKENFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED = "PN_DELIVERYPUSH_NOTIFICATIONFAILED";
    public static final String ERROR_CODE_DELIVERYPUSH_DUPLICATED_ITEMD = "PN_DELIVERYPUSH_DUPLICATED_ITEM";
    public static final String ERROR_CODE_DELIVERYPUSH_END_WORKFLOW_STATUS_NOT_HANDLED = "PN_DELIVERYPUSH_END_WORKFLOW_STATUS_NOT_HANDLED";
    public static final String ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST = "PN_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST";
    public static final String ERROR_CODE_DELIVERYPUSH_DOCUMENT_CREATION_RESPONSE_TYPE_NOT_HANDLED = "PN_DELIVERYPUSH_DOCUMENT_CREATION_RESPONSE_TYPE_NOT_HANDLED";
    public static final String ERROR_CODE_DELIVERYPUSH_TAXID_NOT_VALID = "PN_DELIVERYPUSH_TAXID_NOT_VALID";
    public static final String ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR = "PN_DELIVERYPUSH_READ_FILE_ERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_NORMALIZE_ADDRESS_ERROR = "PN_DELIVERYPUSH_NORMALIZE_ADDRESS_ERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_PHYSICAL_ADDRESS_NOT_PRESENT = "PN_DELIVERYPUSH_PHYSICAL_ADDRESS_NOT_PRESENT";
    public static final String ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND = "PN_DELIVERYPUSH_FILE_NOT_FOUND";

    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED = "PN_DELIVERYPUSH_NOTIFICATION_CANCELLED";

    public enum NotificationRefusedErrorCodeInt {
        FILE_NOTFOUND("FILE_NOTFOUND"),

        FILE_SHA_ERROR( "FILE_SHA_ERROR"),

        FILE_PDF_INVALID_ERROR( "FILE_PDF_INVALID_ERROR"),

        FILE_PDF_TOOBIG_ERROR( "FILE_PDF_TOOBIG_ERROR"),

        TAXID_NOT_VALID("TAXID_NOT_VALID"),

        NOT_VALID_ADDRESS("NOT_VALID_ADDRESS"),

        F24_METADATA_NOT_VALID("F24_METADATA_NOT_VALID"),
        
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE"),

        RECIPIENT_ID_NOT_VALID("RECIPIENT_ID_NOT_VALID");

        private final String value;

        NotificationRefusedErrorCodeInt(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
