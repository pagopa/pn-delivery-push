package it.pagopa.pn.deliverypush.utils;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QrCodeUtils {

  public static byte[] generateQRCodeImage(String text, int width, int height) {
    try {
      Map<EncodeHintType, ?> conf = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
          EncodeHintType.QR_VERSION, 10);
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, conf);

      ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
      MatrixToImageConfig con = new MatrixToImageConfig(0xFF000002, 0xFFFFC041);

      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream, con);
      return pngOutputStream.toByteArray();
    } catch (IOException | WriterException e) {
      throw new PnInternalException(e.getMessage(), ERROR_CODE_PN_GENERIC_ERROR, e);
    }
  }

}
