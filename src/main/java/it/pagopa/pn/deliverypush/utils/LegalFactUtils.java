package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElementV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV28;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

@Slf4j
public class LegalFactUtils {

    private LegalFactUtils() {
    }

    public static LegalFactListElementV28 convert(LegalFactListElementV28 element) {
        LegalFactListElementV28 legalFactListElement = new LegalFactListElementV28();

        LegalFactsIdV28 legalFactsId = getLegalFactsId(element);
        legalFactListElement.setLegalFactsId(legalFactsId);
        legalFactListElement.setIun(element.getIun());
        legalFactListElement.setTaxId(element.getTaxId());

        return legalFactListElement;
    }

    private static LegalFactsIdV28 getLegalFactsId(LegalFactListElementV28 element) {
        LegalFactsIdV28 legalFactsId = new LegalFactsIdV28();
        legalFactsId.setKey(element.getLegalFactsId().getKey());
        LegalFactCategoryV28 category = element.getLegalFactsId().getCategory();

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
