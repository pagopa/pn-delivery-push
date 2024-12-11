package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElementV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV20;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

@Slf4j
public class LegalFactUtils {

    private LegalFactUtils() {
    }

    public static LegalFactListElementV20 convert(LegalFactListElementV20 element) {
        LegalFactListElementV20 legalFactListElement = new LegalFactListElementV20();

        LegalFactsIdV20 legalFactsId = getLegalFactsId(element);
        legalFactListElement.setLegalFactsId(legalFactsId);
        legalFactListElement.setIun(element.getIun());
        legalFactListElement.setTaxId(element.getTaxId());

        return legalFactListElement;
    }

    private static LegalFactsIdV20 getLegalFactsId(LegalFactListElementV20 element) {
        LegalFactsIdV20 legalFactsId = new LegalFactsIdV20();
        legalFactsId.setKey(element.getLegalFactsId().getKey());
        LegalFactCategoryV20 category = element.getLegalFactsId().getCategory();

        legalFactsId.setCategory(category);

        return legalFactsId;
    }

    public static int getNumberOfPageFromPdfBytes(byte[] pdf ) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            log.error("Exception in getNumberOfPageFromPdfBytes for pdf - ex", ex);
            throw new PnInternalException("Cannot get numberOfPages for pdf ", ex.getMessage());
        }
    }
}
