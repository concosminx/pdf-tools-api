package com.nimsoc.tools.pdf.controller;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.nimsoc.tools.pdf.service.FileService;
import com.nimsoc.tools.pdf.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
public class PDFImageController extends BaseController {

  @Autowired
  private final FileService fs;

  public PDFImageController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Endpoint for image added to PDF
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/image/add",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateStampImage(@RequestPart("file") MultipartFile pdfFile,
  @RequestPart("image") MultipartFile image) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    String imageType = FileUtils.detectDocType(image);
    if (imageType == null || !MediaType.IMAGE_PNG_VALUE.equals(imageType)) {
      return ResponseEntity.badRequest().body("Expecting a PNG image".getBytes());
    }


    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest))) {
      ImageData img = ImageDataFactory.create(image.getBytes());

      // Translation defines the position of the image on the page and scale transformation sets image dimensions
      // Please also note that image without scaling is drawn in 1x1 rectangle. And here we draw image on page using
      // its original size in pixels.
      AffineTransform affineTransform = AffineTransform.getTranslateInstance(36, 300);

      // Make sure that the image is visible by concatenating a scale transformation
      affineTransform.concatenate(AffineTransform.getScaleInstance(img.getWidth(), img.getHeight()));

      PdfCanvas canvas = new PdfCanvas(pdfDoc.getFirstPage());
      float[] matrix = new float[6];
      affineTransform.getMatrix(matrix);
      canvas.addImageWithTransformationMatrix(img, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);

    }

    return getPDFDocResponseEntity(dest, "image-add.pdf");
  }


  /**
   * Endpoint for image added to each PDF page
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/image/add/page",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> signPdf(
      @RequestPart("file") MultipartFile pdfFile,
      @RequestPart("image") MultipartFile image
  ) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    String imageType = FileUtils.detectDocType(image);
    if (imageType == null || !MediaType.IMAGE_PNG_VALUE.equals(imageType)) {
      return ResponseEntity.badRequest().body("Expecting a PNG image".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest))) {

      //PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
      //Paragraph paragraph = new Paragraph("My watermark (text)")
      //    .setFont(font)
      //    .setFontSize(30);

      ImageData img = ImageDataFactory.create(image.getBytes());
      float w = img.getWidth();
      float h = img.getHeight();
      PdfExtGState gs1 = new PdfExtGState().setFillOpacity(0.5f);

      // Implement transformation matrix usage in order to scale image
      for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
        PdfPage pdfPage = pdfDoc.getPage(i);
        Rectangle pageSize = pdfPage.getPageSizeWithRotation();

        // When "true": in case the page has a rotation, then new content will be automatically rotated in the
        //      // opposite direction. On the rotated page this would look as if new content ignores page rotation.
        pdfPage.setIgnorePageRotationForContent(true);

        float x = (pageSize.getLeft() + pageSize.getRight()) / 2;
        float y = (pageSize.getTop() + pageSize.getBottom()) / 2;
        PdfCanvas over = new PdfCanvas(pdfDoc.getPage(i));
        over.saveState();
        over.setExtGState(gs1);

        //doc.showTextAligned(paragraph, x, y, i, TextAlignment.CENTER, VerticalAlignment.TOP, 0);
        over.addImageWithTransformationMatrix(img, w, 0, 0, h, x - (w / 2), y - (h / 2), false);
        over.restoreState();
      }
    }


    return getPDFDocResponseEntity(dest, "image-add-page.pdf");
  }


}
