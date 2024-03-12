package com.nimsoc.tools.pdf.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/pdf")
public class TestController {

  /**
   * Endpoint for testing purposes
   *
   * @return
   */
  @GetMapping("/ping")
  public ResponseEntity<String> test() {
    return ResponseEntity.ok("Pong @ " + new Date());
  }

}
