package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class IndustrySentiment {
  private String industry;
  private int positive;
  private int neutral;
  private int negative;
}
