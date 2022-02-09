package it.pagopa.pn.deliverypush.legalfacts;

public class HappenedReceptionAdviceGenerator {

    private final DocumentComposition documentComposition;
    private final CustomInstantWriter instantWriter;

    public HappenedReceptionAdviceGenerator(DocumentComposition documentComposition, CustomInstantWriter instantWriter) {
        this.documentComposition = documentComposition;
        this.instantWriter = instantWriter;
    }

    // FIXME: generare PDF con QrCode e link per AAR cartacei

    // FIXME: generare mail HTML & PlainText con QrCode e link per AAR digitali

}
