package com.nimsoc.tools.pdf.controller;

import com.itextpdf.forms.form.element.Button;
import com.itextpdf.forms.form.element.InputField;
import com.itextpdf.forms.form.element.Radio;
import com.itextpdf.forms.form.element.TextArea;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.nimsoc.tools.pdf.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/pdf")
public class PDFFormController extends BaseController {


  private final FileService fs;

  public PDFFormController(FileService fs) {
    this.fs = fs;
  }

  /**
   * Generate a PDF containing a form
   *
   * @return
   * @throws Exception
   */
  @GetMapping(value = "/form", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> form() throws Exception {
    File tempDir = fs.getTempDir();
    File dest = new File(tempDir, getRandomPdfName());

    try (Document document = new Document(new PdfDocument(new PdfWriter(dest)))) {

      final String[] LANGUAGES = { "English", "German", "French", "Spanish", "Dutch" };

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

      button.setAction(PdfAction.createSubmitForm(
          "http://localhost:8080/book/request", null,
          PdfAction.SUBMIT_HTML_FORMAT | PdfAction.SUBMIT_COORDINATES));

      document.add(table);
      document.add(maleText);
      document.add(femaleText);
      document.add(button);

      PdfDocumentInfo documentInfo = document.getPdfDocument().getDocumentInfo();
      documentInfo.setTitle("New title");
      documentInfo.addCreationDate();
    }

    return getPDFDocResponseEntity(dest, "form.pdf");
  }


}
