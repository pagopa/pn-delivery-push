package it.pagopa.pn.deliverypush.action.details;

import lombok.Data;

import java.util.List;

@Data
public class SequenceItemInternal {
    private String statusCode;
    private List<String> attachments;
}
