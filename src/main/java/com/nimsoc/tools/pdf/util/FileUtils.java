package com.nimsoc.tools.pdf.util;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class FileUtils {

  public static String detectDocType(MultipartFile file) {
    Tika tika = new Tika();
    try {
      return tika.detect(file.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
