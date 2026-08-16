package com.example.demo.newstrend.dto.response;

import lombok.Data;

@Data
public class WordCloudItem {
  private String keyword; // 단어명
  private int score; // 중요도 (20~100)
}
