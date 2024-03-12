package com.nimsoc.tools.pdf.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseController {

  protected final String getRandomPdfName() {
    return "file-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis() + ".pdf";
  }

  protected final String getRandomName() {
    return "f-" + Math.abs(ThreadLocalRandom.current().nextInt()) + "-" + System.currentTimeMillis();
  }

  protected ResponseEntity<byte[]> getPDFDocResponseEntity(File dest, String attachmentName) throws IOException {
    byte[] pdfData = org.apache.commons.io.FileUtils.readFileToByteArray(dest);
    if (dest.exists()) {
      dest.delete();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=" + attachmentName)
        .body(pdfData);
  }

}
