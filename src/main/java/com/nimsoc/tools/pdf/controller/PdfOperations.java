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
import com.itextpdf.layout.Canvas;
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
import com.itextpdf.barcodes.BarcodeEAN;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.barcodes.BarcodeEANSUPP;
import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.barcodes.Barcode1D;
import com.itextpdf.barcodes.BarcodeInter25;
import com.itextpdf.barcodes.BarcodePostnet;
import com.itextpdf.barcodes.Barcode39;
import com.itextpdf.barcodes.BarcodeCodabar;
import com.itextpdf.barcodes.BarcodePDF417;
import com.itextpdf.barcodes.BarcodeDataMatrix;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;


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

  /**
   * Generate a PDF containing barcodes
   * @return
   * @throws Exception
   */
  @GetMapping(value = "generate/barcode", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> generateBarcode() throws Exception {
    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
    Document doc = new Document(pdfDoc, new PageSize(340, 842));

    // The default barcode EAN 13 type
    doc.add(new Paragraph("Barcode EAN.UCC-13"));
    BarcodeEAN codeEAN = new BarcodeEAN(pdfDoc);
    codeEAN.setCode("4512345678906");
    doc.add(new Paragraph("default:"));
    codeEAN.fitWidth(250);
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));
    codeEAN.setGuardBars(false);
    doc.add(new Paragraph("without guard bars:"));
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));
    codeEAN.setBaseline(-1);
    codeEAN.setGuardBars(true);
    doc.add(new Paragraph("text above:"));
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));
    codeEAN.setBaseline(codeEAN.getSize());

    // Barcode EAN UPC A type
    doc.add(new Paragraph("Barcode UCC-12 (UPC-A)"));
    codeEAN.setCodeType(BarcodeEAN.UPCA);
    codeEAN.setCode("785342304749");
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));

    // Barcode EAN 8 type
    doc.add(new Paragraph("Barcode EAN.UCC-8"));
    codeEAN.setCodeType(BarcodeEAN.EAN8);
    codeEAN.setBarHeight(codeEAN.getSize() * 1.5f);
    codeEAN.setCode("34569870");
    codeEAN.fitWidth(250);
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));

    // Barcode UPC E type
    doc.add(new Paragraph("Barcode UPC-E"));
    codeEAN.setCodeType(BarcodeEAN.UPCE);
    codeEAN.setCode("03456781");
    codeEAN.fitWidth(250);
    doc.add(new Image(codeEAN.createFormXObject(pdfDoc)));
    codeEAN.setBarHeight(codeEAN.getSize() * 3);

    // Barcode EANSUPP type
    doc.add(new Paragraph("Bookland - BarcodeEANSUPP"));
    doc.add(new Paragraph("ISBN 0-321-30474-8"));
    codeEAN = new BarcodeEAN(pdfDoc);
    codeEAN.setCodeType(BarcodeEAN.EAN13);
    codeEAN.setCode("9781935182610");
    BarcodeEAN codeSUPP = new BarcodeEAN(pdfDoc);
    codeSUPP.setCodeType(BarcodeEAN.SUPP5);
    codeSUPP.setCode("55999");
    codeSUPP.setBaseline(-2);
    BarcodeEANSUPP eanSupp = new BarcodeEANSUPP(codeEAN, codeSUPP);
    doc.add(new Image(eanSupp.createFormXObject(null, ColorConstants.BLUE, pdfDoc)));

    // Barcode CODE 128 type
    doc.add(new Paragraph("Barcode 128"));
    Barcode128 code128 = new Barcode128(pdfDoc);
    code128.setCode("0123456789 hello");
    code128.fitWidth(250);
    doc.add(new Image(code128.createFormXObject(pdfDoc))
            .setRotationAngle(Math.PI / 2)
            .setMargins(10, 10, 10, 10));
    code128.setCode("0123456789\uffffMy Raw Barcode (0 - 9)");
    code128.setCodeType(Barcode128.CODE128_RAW);
    code128.fitWidth(250);
    doc.add(new Image(code128.createFormXObject(pdfDoc)));

    // Data for the barcode
    String code402 = "24132399420058289";
    String code90 = "3700000050";
    String code421 = "422356";
    StringBuffer data = new StringBuffer(code402);
    data.append(Barcode128.FNC1);
    data.append(code90);
    data.append(Barcode128.FNC1);
    data.append(code421);

    Barcode128 shipBarCode = new Barcode128(pdfDoc);
    shipBarCode.setX(0.75f);
    shipBarCode.setN(1.5f);
    shipBarCode.setSize(10f);
    shipBarCode.setTextAlignment(Barcode1D.ALIGN_CENTER);
    shipBarCode.setBaseline(10f);
    shipBarCode.setBarHeight(50f);
    shipBarCode.setCode(data.toString());
    shipBarCode.fitWidth(250);
    doc.add(new Image(shipBarCode.createFormXObject(ColorConstants.BLACK, ColorConstants.BLUE, pdfDoc)));

    // CODE 128 type barcode, which is composed of 3 blocks with AI 01, 3101 and 10
    Barcode128 uccEan128 = new Barcode128(pdfDoc);
    uccEan128.setCodeType(Barcode128.CODE128_UCC);
    uccEan128.setCode("(01)00000090311314(10)ABC123(15)060916");
    uccEan128.fitWidth(250);
    doc.add(new Image(uccEan128.createFormXObject(ColorConstants.BLUE, ColorConstants.BLACK, pdfDoc)));
    uccEan128.setCode("0191234567890121310100035510ABC123");
    uccEan128.fitWidth(250);
    doc.add(new Image(uccEan128.createFormXObject(ColorConstants.BLUE, ColorConstants.RED, pdfDoc)));
    uccEan128.setCode("(01)28880123456788");
    uccEan128.fitWidth(250);
    doc.add(new Image(uccEan128.createFormXObject(ColorConstants.BLUE, ColorConstants.BLACK, pdfDoc)));

    // Barcode INTER25 type
    doc.add(new Paragraph("Barcode Interrevealed 2 of 5"));
    BarcodeInter25 code25 = new BarcodeInter25(pdfDoc);
    code25.setGenerateChecksum(true);
    code25.setCode("41-1200076041-001");
    code25.fitWidth(250);
    doc.add(new Image(code25.createFormXObject(pdfDoc)));
    code25.setCode("411200076041001");
    code25.fitWidth(250);
    doc.add(new Image(code25.createFormXObject(pdfDoc)));
    code25.setCode("0611012345678");
    code25.setChecksumText(true);
    code25.fitWidth(250);
    doc.add(new Image(code25.createFormXObject(pdfDoc)));

    // Barcode POSTNET type
    doc.add(new Paragraph("Barcode Postnet"));
    BarcodePostnet codePost = new BarcodePostnet(pdfDoc);
    doc.add(new Paragraph("ZIP"));
    codePost.setCode("01234");
    codePost.fitWidth(250);
    doc.add(new Image(codePost.createFormXObject(pdfDoc)));
    doc.add(new Paragraph("ZIP+4"));
    codePost.setCode("012345678");
    codePost.fitWidth(250);
    doc.add(new Image(codePost.createFormXObject(pdfDoc)));
    doc.add(new Paragraph("ZIP+4 and dp"));
    codePost.setCode("01234567890");
    codePost.fitWidth(250);
    doc.add(new Image(codePost.createFormXObject(pdfDoc)));

    // Barcode PLANET type
    doc.add(new Paragraph("Barcode Planet"));
    BarcodePostnet codePlanet = new BarcodePostnet(pdfDoc);
    codePlanet.setCode("01234567890");
    codePlanet.setCodeType(BarcodePostnet.TYPE_PLANET);
    codePlanet.fitWidth(250);
    doc.add(new Image(codePlanet.createFormXObject(pdfDoc)));

    // Barcode CODE 39 type
    doc.add(new Paragraph("Barcode 3 of 9"));
    Barcode39 code39 = new Barcode39(pdfDoc);
    code39.setCode("ITEXT IN ACTION");
    code39.fitWidth(250);
    doc.add(new Image(code39.createFormXObject(pdfDoc)));

    doc.add(new Paragraph("Barcode 3 of 9 extended"));
    Barcode39 code39ext = new Barcode39(pdfDoc);
    code39ext.setCode("iText in Action");
    code39ext.setStartStopText(false);
    code39ext.setExtended(true);
    code39ext.fitWidth(250);
    doc.add(new Image(code39ext.createFormXObject(pdfDoc)));

    // Barcode CODABAR type
    doc.add(new Paragraph("Codabar"));
    BarcodeCodabar codabar = new BarcodeCodabar(pdfDoc);
    codabar.setCode("A123A");
    codabar.setStartStopText(true);
    codabar.fitWidth(250);
    doc.add(new Image(codabar.createFormXObject(pdfDoc)));

    doc.add(new AreaBreak());

    // Barcode PDF417 type
    doc.add(new Paragraph("Barcode PDF417"));
    BarcodePDF417 pdf417 = new BarcodePDF417();
    String text = "Call me Ishmael. Some years ago--never mind how long "
            + "precisely --having little or no money in my purse, and nothing "
            + "particular to interest me on shore, I thought I would sail about "
            + "a little and see the watery part of the world.";
    pdf417.setCode(text);

    PdfFormXObject xObject = pdf417.createFormXObject(pdfDoc);
    Image img = new Image(xObject);
    doc.add(img.setAutoScale(true));

    doc.add(new Paragraph("Barcode Datamatrix"));
    BarcodeDataMatrix datamatrix = new BarcodeDataMatrix();
    datamatrix.setCode(text);
    Image imgDM = new Image(datamatrix.createFormXObject(pdfDoc));
    doc.add(imgDM.scaleToFit(250, 250));

    // Barcode QRCode type
    doc.add(new Paragraph("Barcode QRCode"));
    BarcodeQRCode qrcode = new BarcodeQRCode("Moby Dick by Herman Melville");
    img = new Image(qrcode.createFormXObject(pdfDoc));
    doc.add(img.scaleToFit(250, 250));

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
   * Endpoint for image added to PDF
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/add/image",
          consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
          produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> addImage(
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

    PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest));
    Image img = new Image(ImageDataFactory.create(image.getBytes()));
    Document doc = new Document(pdfDoc, new PageSize(img.getImageWidth(), img.getImageHeight()));

    img.setFixedPosition(0, 0);
    doc.add(img);

    // Added a custom shape on top of a image
    PdfCanvas canvas = new PdfCanvas(pdfDoc.getFirstPage());
    canvas.setStrokeColor(ColorConstants.RED)
            .setLineWidth(3)
            .moveTo(220, 330)
            .lineTo(240, 370)
            .arc(200, 350, 240, 390, 0, 180)
            .lineTo(220, 330)
            .closePathStroke()
            .setFillColor(ColorConstants.RED)
            .circle(220, 370, 10)
            .fill();

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


  /**
   * Endpoint for metadata added to PDF
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/add/metadata",
          consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
          produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> addMetadata(
          @RequestPart("file") MultipartFile pdfFile
  ) throws Exception {
    String fileType = FileUtils.detectDocType(pdfFile);
    if (fileType == null || !MediaType.APPLICATION_PDF_VALUE.equals(fileType)) {
      return ResponseEntity.badRequest().body("Expecting a PDF file".getBytes());
    }

    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()),
            new PdfWriter(dest.getAbsolutePath(), new WriterProperties().addXmpMetadata()));
    PdfDocumentInfo info = pdfDoc.getDocumentInfo();
    info.setTitle("New title");
    info.addCreationDate();

    pdfDoc.close();

    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=signed.pdf")
            .body(pdfData);
  }

  /**
   * Endpoint for text added to PDF
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/add/text",
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

    PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfFile.getInputStream()), new PdfWriter(dest));

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

    pdfDoc.close();


    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=signed.pdf")
            .body(pdfData);
  }


  private static void drawText(PdfCanvas canvas, PdfDocument pdfDoc, Rectangle pageSize, float x, float y, double rotation) {
    Canvas canvasDrawText = new Canvas(canvas, pageSize)
            .showTextAligned("This is some extra text added to the left of the page",
                    x, y, TextAlignment.CENTER, (float) Math.toRadians(rotation));
    canvasDrawText.close();
  }

  private String getRandomPdfName() {
    return "file-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis() + ".pdf";
  }

  private String getRandomName() {
    return "f-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis();
  }
}
