package it.pagopa.pn.deliverypush.action.utils;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class QrCodeUtilsTest {

  @Test
  void generateQRCodeWithLongUrlWithLErrorLevel() throws IOException, NotFoundException {

    var url = "https://cittadini.hotfix.notifichedigitali.it/" +
            "auth/login" + "?aar=" +
            "TFVOWi1WQU1QLVZZUFQtMjAyNDA0LUUtMV9QRi1hMDFiNjJkNC1lM2Y4LTQ4ZjMtYTRkOC1jZjYyOGEzNGI3NDVfOWEyNDQwMjEtZTExOS00MjZkLWE1OTAtOTlkZDZjNWZhYjQ4";

    byte[] qrCode = QrCodeUtils.generateQRCodeImage(url, 160, 160, ErrorCorrectionLevel.L);

    BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
            new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(qrCode)))));

    Result result = new MultiFormatReader().decode(binaryBitmap);

    assertEquals(url, result.getText());
  }

  @Test
  void generateQRCodeImage() throws IOException, NotFoundException {

    byte[] qrCode = QrCodeUtils.generateQRCodeImage("test", 10, 10, ErrorCorrectionLevel.H);


    BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
        new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(qrCode)))));

    Result result = new MultiFormatReader().decode(binaryBitmap);

    assertEquals("test", result.getText());


  }

  @Test
  void generateQRCodeImageFailure() throws IOException, NotFoundException {
    try (MockedStatic<MatrixToImageWriter> mockWriter =
        Mockito.mockStatic(MatrixToImageWriter.class);) {
      mockWriter.when(() -> MatrixToImageWriter.writeToStream(any(), any(), any()))
          .thenThrow(new IOException("message"));
      assertThrows(PnInternalException.class,
          () -> QrCodeUtils.generateQRCodeImage("test", 10, 10, ErrorCorrectionLevel.H));
    }
  }


}
