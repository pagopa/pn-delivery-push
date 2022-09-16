package it.pagopa.pn.deliverypush.sanitizers;

import java.util.Map;

public class DefaultHtmlSanitize extends HtmlSanitizer{

    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        return templateModelMap;
    }
}
