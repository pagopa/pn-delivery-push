package it.pagopa.pn.deliverypush.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FontUtils {
    private static final String FONT_BASE_PATH = "documents_composition_templates/fonts/";
    private static final String TITILIUM_BASE_PATH = FONT_BASE_PATH + "Titillium_Web/";
    private static final String MONTSERRAT_BASE_PATH = FONT_BASE_PATH + "Montserrat/";
    private static final String MONTSERRAT_STATIC_BASE_PATH = MONTSERRAT_BASE_PATH + "static/";

    private static final String TITILIUM_FAMILY = "Titillium Web";
    private static final String MONTSERRAT_FAMILY = "Montserrat";

    private static ArrayList<FontType> fontList = new ArrayList<>(Arrays.asList(
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-Black.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-Bold.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-BoldItalic.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-ExtraLight.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-Italic.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-Light.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-LightItalic.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-Regular.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-SemiBold.ttf", TITILIUM_FAMILY),
            new FontType(TITILIUM_BASE_PATH + "TitilliumWeb-SemiBoldItalic.ttf", TITILIUM_FAMILY),
            new FontType(MONTSERRAT_BASE_PATH + "Montserrat-Italic-VariableFont_wght.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_BASE_PATH + "Montserrat-VariableFont_wght.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Black.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-BlackItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Bold.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-BoldItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-ExtraBold.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-ExtraBoldItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-ExtraLight.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-ExtraLightItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Italic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Light.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-LightItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Medium.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-MediumItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Regular.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-SemiBold.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-SemiBoldItalic.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-Thin.ttf", MONTSERRAT_FAMILY),
            new FontType(MONTSERRAT_STATIC_BASE_PATH + "Montserrat-ThinItalic.ttf", MONTSERRAT_FAMILY)
            ));
    
    public static List<FontType> getFontList(){
        return fontList;
    }
}
