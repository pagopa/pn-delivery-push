package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import org.springframework.util.StringUtils;

public enum SendAttachmentMode {
    AAR,
    AAR_DOCUMENTS,
    AAR_DOCUMENTS_PAYMENTS;

    public static SendAttachmentMode fromValue(String sendAnalogNotificationAttachments) {
        if(StringUtils.hasText(sendAnalogNotificationAttachments)) {
            try {
                return SendAttachmentMode.valueOf(sendAnalogNotificationAttachments);
            }catch (Exception e){
                return AAR;
            }
        }
        return AAR;
    }
}
