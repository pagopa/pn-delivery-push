package it.pagopa.pn.deliverypush.legalfacts;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;

class DocumentCompositionTest {

    private DocumentComposition.TemplateType templateType;

    private Map<DocumentComposition.TemplateType, String> baseUris;

    private Configuration freemarker;

    private DocumentComposition documentComposition;

    @BeforeEach
    public void setup() throws IOException {
        documentComposition = new DocumentComposition(freemarker);
    }

    @Test
    void executeTextTemplate() throws IOException {

        Mockito.when(freemarker.getTemplate(Mockito.any())).thenReturn(Mockito.any());

        String tmp = documentComposition.executeTextTemplate(Mockito.any(), Mockito.any());

        System.out.println("TEMP : " + tmp);
    }

    @Test
    void executePdfTemplate() {
    }

    @Test
    void getNumberOfPageFromPdfBytes() {
    }
}