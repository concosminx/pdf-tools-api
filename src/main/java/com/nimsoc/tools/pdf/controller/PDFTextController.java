package com.nimsoc.tools.pdf.controller;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.nimsoc.tools.pdf.service.FileService;
import com.nimsoc.tools.pdf.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/pdf")
public class PDFTextController extends BaseController {

  private final FileService fs;

  public PDFTextController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Endpoint for text added to PDF
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/text/page",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> addText(
      @RequestPart("file") MultipartFile pdfFile
  ) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest))) {

      for (int p = 1; p <= pdfDoc.getNumberOfPages(); p++) {
        Rectangle pageSize = pdfDoc.getPage(p).getPageSize();

        PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(p));

        // In case the page has a rotation, then new content will be automatically rotated.
        // Such an automatic rotation means, that we should consider page as if it's not rotated.
        // This is the particular case for the page 3 below
        if (p == 3) {

          // The width of the page rotated by 90 degrees corresponds to the height of the unrotated one.
          // The left side of the page rotated by 90 degrees corresponds to the bottom of the unrotated page.
          drawText(canvas, pdfDoc, pageSize, pageSize.getWidth() / 2, 18, 180);
          drawText(canvas, pdfDoc, pageSize, pageSize.getWidth() / 2, 34, 180);
        } else {
          drawText(canvas, pdfDoc, pageSize, pageSize.getLeft() + 18,
              (pageSize.getTop() + pageSize.getBottom()) / 2, 90);
          drawText(canvas, pdfDoc, pageSize, pageSize.getLeft() + 34,
              (pageSize.getTop() + pageSize.getBottom()) / 2, 90);
        }
      }
    }


    return getPDFDocResponseEntity(dest, "text.pdf");
  }

  /**
   * Endpoint for text Stamp
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/text/stamp",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateStamp(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());


    try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest)); Document doc = new Document(pdfDoc)) {
      Paragraph header = new Paragraph("Copy")
          .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
          .setFontSize(14)
          .setFontColor(ColorConstants.RED);

      for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
        Rectangle pageSize = pdfDoc.getPage(i).getPageSize();
        float x = pageSize.getWidth() / 2;
        float y = pageSize.getTop() - 20;
        doc.showTextAligned(header, x, y, i, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);
      }
    }

    return getPDFDocResponseEntity(dest, "stamp.pdf");
  }

  private static void drawText(PdfCanvas canvas, PdfDocument pdfDoc, Rectangle pageSize, float x, float y, double rotation) {
    Canvas canvasDrawText = new Canvas(canvas, pageSize)
        .showTextAligned("This is some extra text added to the left of the page",
            x, y, TextAlignment.CENTER, (float) Math.toRadians(rotation));
    canvasDrawText.close();
  }
}
