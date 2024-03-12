package com.nimsoc.tools.pdf.controller;

import com.itextpdf.kernel.pdf.*;
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
public class PDFEncryptController extends BaseController {


  private final FileService fs;

  public PDFEncryptController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Endpoint for PDF encryption
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/encrypt",
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

    return getPDFDocResponseEntity(dest, "encrypted.pdf");
  }
}
