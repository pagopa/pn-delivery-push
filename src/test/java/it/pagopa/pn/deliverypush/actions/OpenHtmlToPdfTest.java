package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

class OpenHtmlToPdfTest {

	@Test
	void testOpenHtmlToPdf() throws Exception {
				
		Path resourceDirectory = Paths.get("src", "main", "resources", "openhtmltopdf");
		String absolutePath = resourceDirectory.toFile().getAbsolutePath();
		 		
		StringBuilder sb = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(absolutePath + "/openHtmlToPdf.html"), StandardCharsets.UTF_8)) {
			stream.forEach(s -> sb.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		String html = sb.toString();
		
		ByteArrayOutputStream renderedPdfBytes = new ByteArrayOutputStream();
		PdfRendererBuilder builder = new PdfRendererBuilder();
		
		String baseUri = FileSystems.getDefault().getPath(absolutePath).toUri().toString();
		
		builder.withHtmlContent(html, baseUri);
		builder.toStream(renderedPdfBytes);
		builder.run();
		renderedPdfBytes.close();
		
		byte[] renderedPdf = renderedPdfBytes.toByteArray();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").withZone(ZoneId.of("Europe/Rome"));
        String formattedDate = dtf.format(Instant.now()); 
		String outFileName = absolutePath + "/" + formattedDate + "_openHtmlToPdf.pdf";
		
		try (FileOutputStream fos = new FileOutputStream(outFileName)) {
		    fos.write(renderedPdf);
		}
		
		Assertions.assertTrue( Files.exists(Paths.get(outFileName)) );
    }

}

