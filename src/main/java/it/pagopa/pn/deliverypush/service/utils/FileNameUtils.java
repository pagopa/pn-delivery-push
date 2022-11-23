package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.utils.MimeTypesUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileNameUtils {
    
    /**
     * il nome, viene generato da iun, type e factid e per ora si suppone essere un pdf
     * @param iun iun
     * @param fileType file type
     * @param fileId file id
     * @return filename
     */
    public static String buildFileName(String iun, String fileType, String fileId, String contentType)
    {
        String extension = "pdf";
        try{
            extension = MimeTypesUtils.getDefaultExt(contentType);
        } catch (Exception e)
        {
            log.warn("right extension not found, using PDF");
        }

        return iun.replaceAll("[^a-zA-Z0-9]", "")
                + "_" + fileType
                + "_" + fileId.replace(SAFE_STORAGE_URL_PREFIX, "").replaceAll("[^a-zA-Z0-9]", "")
                + "." + extension;
    }
}
