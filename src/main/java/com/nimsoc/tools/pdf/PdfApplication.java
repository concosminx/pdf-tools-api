package com.nimsoc.tools.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PdfApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(PdfApplication.class, args);
    configurableApplicationContext.registerShutdownHook();
  }

}
