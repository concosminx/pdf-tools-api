package com.nimsoc.tools.pdf.controller;

import com.itextpdf.barcodes.BarcodeEAN;
import com.itextpdf.commons.utils.FileUtil;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormCreator;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.forms.fields.properties.SignedAppearanceText;
import com.itextpdf.forms.form.element.*;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfPadesSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignerProperties;
import com.nimsoc.tools.pdf.service.FileService;
import com.nimsoc.tools.pdf.util.FileUtils;
import net.lingala.zip4j.ZipFile;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/pdf")
public class PdfOperations {

  @Value("${keystore.password}")
  private String keyStorePassword;

  @Autowired
  private FileService fs;

  /**
   * Endpoint for testing purposes
   *
   * @return
   */
  @GetMapping("/ping")
  public ResponseEntity<String> test() {
    return ResponseEntity.ok("Pong @ " + new Date());
  }

  /**
   * Generate a PDF containing a form
   * @return
   * @throws Exception
   */
  @GetMapping(value = "generate/form", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateForm() throws Exception {
    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    Document document = new Document(new PdfDocument(new PdfWriter(dest)));

    InputField inputField = new InputField("input field");
    inputField.setValue("John");
    inputField.setInteractive(true);

    TextArea textArea = new TextArea("text area");
    textArea.setValue("I'm a chess player.\n"
        + "In future I want to compete in professional chess and be the world champion.\n"
        + "My favorite opening is caro-kann.\n"
        + "Also I play sicilian defense a lot.");
    textArea.setInteractive(true);

    Table table = new Table(2, false);
    table.addCell("Name:");
    table.addCell(new Cell().add(inputField));
    table.addCell("Personal info:");
    table.addCell(new Cell().add(textArea));



    Radio male = new Radio("male", "radioGroup");
    male.setChecked(false);
    male.setInteractive(true);
    male.setBorder(new SolidBorder(1));

    Paragraph maleText = new Paragraph("Male: ");
    maleText.add(male);

    Radio female = new Radio("female", "radioGroup");
    female.setChecked(true);
    female.setInteractive(true);
    female.setBorder(new SolidBorder(1));

    Paragraph femaleText = new Paragraph("Female: ");
    femaleText.add(female);

    Button button = new Button("submit");
    button.setValue("Submit");
    button.setInteractive(true);
    button.setBorder(new SolidBorder(2));
    button.setWidth(50);
    button.setBackgroundColor(ColorConstants.LIGHT_GRAY);

    document.add(table);
    document.add(maleText);
    document.add(femaleText);
    document.add(button);

    document.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=generated.pdf")
        .body(pdfData);
  }


  /**
   * Generate a PDF with a table
   * @return
   * @throws Exception
   */
  @GetMapping(value = "generate/table", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateTable() throws Exception {
    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    List<List<String>> data = new ArrayList<>();
    String[] tableTitleList = {" Title", " (Re)set", " Obs", " Mean", " Std.Dev", " Min", " Max", "Unit"};
    data.add(Arrays.asList(tableTitleList));
    for (int i = 0; i < 10; i++) {
      List<String> dataLine = new ArrayList<>();
      for (int j = 0; j < tableTitleList.length; j++) {
        dataLine.add(tableTitleList[j] + " " + (i + 1));
      }
      data.add(dataLine);
    }

    PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
    Document doc = new Document(pdfDoc);

    Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

    List<List<String>> dataset = data;
    for (List<String> record : dataset) {
      for (String field : record) {
        table.addCell(new Cell().add(new Paragraph(field)));
      }
    }

    doc.add(table);

    doc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=generated.pdf")
        .body(pdfData);
  }


  /**
   * Endpoint for Barcode generation
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/generate/barcode",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateBarcode(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest));
    for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
      PdfPage pdfPage = pdfDoc.getPage(i);
      Rectangle pageSize = pdfPage.getPageSize();
      float x = pageSize.getLeft() + 10;
      float y = pageSize.getTop() - 50;
      BarcodeEAN barcode = new BarcodeEAN(pdfDoc);
      barcode.setCodeType(BarcodeEAN.EAN8);
      barcode.setCode(createBarcodeNumber(i));

      PdfFormXObject barcodeXObject = barcode.createFormXObject(ColorConstants.BLACK, ColorConstants.BLACK, pdfDoc);
      PdfCanvas over = new PdfCanvas(pdfPage);
      over.addXObjectAt(barcodeXObject, x, y);
    }

    pdfDoc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=barcode.pdf")
        .body(pdfData);
  }


  /**
   * Endpoint for Barcode generation
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/generate/stamp",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateStamp(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest));
    Document doc = new Document(pdfDoc);

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

    doc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=stamp.pdf")
        .body(pdfData);
  }

  private static String createBarcodeNumber(int i) {
    String barcodeNumber = String.valueOf(i);
    barcodeNumber = "00000000".substring(barcodeNumber.length()) + barcodeNumber;

    return barcodeNumber;
  }

  /**
   * Endpoint for Barcode encryption
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/generate/encrypt",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateEncrypted(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(
        new PdfReader(pdfFile.getInputStream()),
        new PdfWriter(dest.getAbsolutePath(), new WriterProperties().setStandardEncryption(
            "user".getBytes(),
            "pass".getBytes(),
            EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.ENCRYPTION_AES_128 | EncryptionConstants.DO_NOT_ENCRYPT_METADATA))
    );

    pdfDoc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=stamp.pdf")
        .body(pdfData);
  }

  /**
   * Endpoint for Barcode encryption
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/generate/stampimage",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateStampImage(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest));
    ImageData image = ImageDataFactory.create(new ClassPathResource("images/hero.jpg").getContentAsByteArray());

    // Translation defines the position of the image on the page and scale transformation sets image dimensions
    // Please also note that image without scaling is drawn in 1x1 rectangle. And here we draw image on page using
    // its original size in pixels.
    AffineTransform affineTransform = AffineTransform.getTranslateInstance(36, 300);

    // Make sure that the image is visible by concatenating a scale transformation
    affineTransform.concatenate(AffineTransform.getScaleInstance(image.getWidth(), image.getHeight()));

    PdfCanvas canvas = new PdfCanvas(pdfDoc.getFirstPage());
    float[] matrix = new float[6];
    affineTransform.getMatrix(matrix);
    canvas.addImageWithTransformationMatrix(image, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
    pdfDoc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=stamp-image.pdf")
        .body(pdfData);
  }

  /**
   * Endpoint for PDF splitting
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/split",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = "application/zip")
  public ResponseEntity<byte[]> splitPdf(@RequestPart("file") MultipartFile pdfFile) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();

    final String dest = "splitDocument1_%s.pdf";
    File workDir = new File(tempDir, getRandomName());
    if (!workDir.exists()) {
      workDir.mkdirs();
    }

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()));

    List<PdfDocument> splitDocuments = new PdfSplitter(pdfDoc) {
      int partNumber = 1;
      @Override
      protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
        try {
          FileOutputStream fos = new FileOutputStream(new File(workDir, String.format(dest, partNumber++)));
          return new PdfWriter(fos);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }.splitByPageCount(3);

    for (PdfDocument doc : splitDocuments) {
      doc.close();
    }

    pdfDoc.close();

    File zip = new File(tempDir, getRandomName() + ".zip");
    new ZipFile(zip).addFolder(workDir);

    byte[] zipData = org.apache.commons.io.FileUtils.readFileToByteArray(zip);

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=archive.zip")
        .body(zipData);
  }

  /**
   * Endpoint for PDF signing
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/watermark/image",
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

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest));
    Document doc = new Document(pdfDoc);

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
      // opposite direction. On the rotated page this would look as if new content ignores page rotation.
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

    doc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=signed.pdf")
        .body(pdfData);
  }

  private String getRandomPdfName() {
    return "file-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis() + ".pdf";
  }

  private String getRandomName() {
    return "f-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis();
  }
}
