package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;

public class PnDeliveryPushExceptionCodes extends PnExceptionsCodes {

    // raccolgo qui tutti i codici di errore di delivery push
    public static final String ERROR_CODE_DELIVERYPUSH_NOTFOUND = "PN_DELIVERYPUSH_NOTFOUND";
    public static final String ERROR_CODE_DELIVERYPUSH_GETFILEERROR = "PN_DELIVERYPUSH_GETFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR = "PN_DELIVERYPUSH_UPLOADFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR = "PN_DELIVERYPUSH_UPDATEMETAFILEERROR";
    public static final String ERROR_CODE_DELIVERYPUSH_ERRORCOMPUTECHECKSUM = "PN_DELIVERYPUSH_ERRORCOMPUTECHECKSUM";
    public static final String ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED = "PN_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED";


    public static final String ERROR_CODE_WEBHOOK_UPDATEEVENTSTREAM = "PN_WEBHOOK_UPDATEEVENTSTREAM";
    public static final String ERROR_CODE_WEBHOOK_CONSUMEEVENTSTREAM = "PN_WEBHOOK_CONSUMEEVENTSTREAM";
}
