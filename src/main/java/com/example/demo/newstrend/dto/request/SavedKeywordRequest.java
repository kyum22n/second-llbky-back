package com.example.demo.newstrend.dto.request;

import lombok.Data;

@Data
public class SavedKeywordRequest {
  private int memberId;
  private String keyword;
  private String sourceLabel;
}
