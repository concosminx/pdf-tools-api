package com.nimsoc.tools.pdf.controller;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nimsoc.tools.pdf.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/pdf")
public class PDFTableController extends BaseController {

  private final FileService fs;

  public PDFTableController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Generates a PDF with a table
   *
   * @return
   * @throws Exception
   */
  @GetMapping(value = "/table", produces = MediaType.APPLICATION_PDF_VALUE)
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
    try (Document doc = new Document(pdfDoc)) {

      Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

      List<List<String>> dataset = data;
      for (List<String> record : dataset) {
        for (String field : record) {
          table.addCell(new Cell().add(new Paragraph(field)));
        }
      }

      doc.add(table);
    }


    return getPDFDocResponseEntity(dest, "table.pdf");
  }
}
