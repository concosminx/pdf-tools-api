package com.nimsoc.tools.pdf.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.nimsoc.tools.pdf.service.FileService;
import com.nimsoc.tools.pdf.util.FileUtils;
import net.lingala.zip4j.ZipFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

@RestController
@RequestMapping("/pdf")
public class PDFSplitController extends BaseController {

  private final FileService fs;

  public PDFSplitController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Endpoint for PDF splitting
   *
   * @param pdfFile
   * @return
   * @throws Exception
   */
  @PostMapping(value = "/split", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/zip")
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

    org.apache.commons.io.FileUtils.deleteDirectory(workDir);

    byte[] zipData = org.apache.commons.io.FileUtils.readFileToByteArray(zip);

    if (zip.exists()) {
      zip.delete();
    }

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=archive.zip")
        .body(zipData);
  }


}
