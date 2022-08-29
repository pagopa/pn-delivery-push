package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;

public interface SafeStorageService {

    FileDownloadResponseInt getFile(String fileKey, Boolean metadataOnly) ;

    FileCreationResponseInt createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);
}
