package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import org.springframework.util.StringUtils;

public enum SendAttachmentMode {
    AAR("AAR"),
    AAR_DOCUMENTS("AAR-DOCUMENTS"),
    AAR_DOCUMENTS_PAYMENTS("AAR-DOCUMENTS-PAYMENTS");

    public static SendAttachmentMode fromValue(String sendAnalogNotificationAttachments) {
        if(StringUtils.hasText(sendAnalogNotificationAttachments)) {
            try {
                // gestisce le varianti con il "-" come separatore, o ritorna direttamente il valore dell'enum
                return "AAR-DOCUMENTS-PAYMENTS".equals(sendAnalogNotificationAttachments)?AAR_DOCUMENTS_PAYMENTS:("AAR-DOCUMENTS".equals(sendAnalogNotificationAttachments)?AAR_DOCUMENTS:SendAttachmentMode.valueOf(sendAnalogNotificationAttachments));
            }catch (Exception e){
                return AAR;
            }
        }
        return AAR;
    }

    private final String value;

    SendAttachmentMode(String value) {
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
