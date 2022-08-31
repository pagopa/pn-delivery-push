package it.pagopa.pn.deliverypush.dto.ext.safestorage;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class FileDownloadResponseInt {
    private String key;
    private String checksum;
    private BigDecimal contentLength;
    private String contentType;
    private FileDownloadInfoInt download;
}
