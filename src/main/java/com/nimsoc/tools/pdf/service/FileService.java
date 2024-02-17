package com.nimsoc.tools.pdf.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@Lazy(false)
@Slf4j
public class FileService implements InitializingBean, DisposableBean {
  private File tempDir;

  @Override
  public void afterPropertiesSet() throws Exception {
    tempDir = Files.createTempDirectory("pdfFileServer").toFile();
    log.info("Started with temp folder {}", tempDir);
  }

  public File getTempDir() {
    return tempDir;
  }

  @Override
  public void destroy() throws Exception {
    boolean deleted = FileSystemUtils.deleteRecursively(tempDir);
    log.info("Deleted temp folder", deleted);
  }
}
