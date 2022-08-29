package it.pagopa.pn.deliverypush.dto.ext.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreationWithContentRequest extends FileCreationRequest {
    private byte[] content;
}
