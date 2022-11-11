package it.pagopa.pn.deliverypush.action.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;

class QrCodeUtilsTest {

  @Test
  void generateQRCodeImage() throws IOException, NotFoundException {

    byte[] qrCode = QrCodeUtils.generateQRCodeImage("test", 10, 10);


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
          .thenThrow(IOException.class);
      assertThrows(PnInternalException.class,
          () -> QrCodeUtils.generateQRCodeImage("test", 10, 10));

    }
  }


}
