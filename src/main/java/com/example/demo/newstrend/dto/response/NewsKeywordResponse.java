package com.example.demo.newstrend.dto.response;

import lombok.Data;

/*
AI가 추출한 키워드 하나를 표현하는 DTO

List<NewsKeywordResponse> 형태로 묶여 전달됨
*/

@Data
public class NewsKeywordResponse {
    private String keyword;
   
}
