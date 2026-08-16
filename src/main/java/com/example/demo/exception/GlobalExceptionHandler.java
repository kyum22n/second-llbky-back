package com.example.demo.exception;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	// 잘못된 파라미터일 경우
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
		Map<String, String> map = new HashMap<>();
		map.put("message", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map); // 400
	}

	// 데이터 값을 찾을 수 없을 경우
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, Object>> handleNoSuchElementException(NoSuchElementException e) {
		Map<String, Object> map = new HashMap<>();
		map.put("message", e.getMessage() != null ? e.getMessage() : "데이터를 찾을 수 없습니다");

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map); // 404
	}

	// 중복된 값일 경우
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<Map<String, Object>> handleDuplicateKeyException(DuplicateKeyException e) {
		Map<String, Object> map = new HashMap<>();
		map.put("message", e.getMessage() != null ? e.getMessage() : "이미 존재하는 값입니다");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(map); // 409
	}

	// 제약조건에 맞지 않아 상태가 달라 처리가 실패할 경우(산책모집, 채팅, 리뷰 등록시 등)
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalStateException(
			IllegalStateException e) {
		Map<String, Object> map = new HashMap<>();
		map.put("message", e.getMessage() != null ? e.getMessage() : "현재 처리 상태가 맞지 않아 처리 할 수 없습니다.");

		return ResponseEntity.status(HttpStatus.CONFLICT).body(map); // 409

	}

	// 제약조건 위반, SQL 문법 오류, 커넥션 문제 등 발생할 경우(DB 관련)
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException e) {
		Map<String, Object> map = new HashMap<>();
		map.put("message", e.getMessage() != null ? e.getMessage() : "데이터베이스 처리 중 오류가 발생했습니다");

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map); // 500

	}

	// 유효성 검사 예외 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException e) {
		Map<String, Object> map = new HashMap<>();

		// 발생하는 에러들을 리스트로 처리해줌
		List<FieldError> list = e.getBindingResult().getFieldErrors();

		// 에러가 여러 개면 메시지도 여러 개를 처리해주어야 함
		JSONArray jsonArray = new JSONArray(); // 대괄호 만듦
		for (FieldError fe : list) {
			JSONObject jsonObject = new JSONObject(); // 중괄호 만듦
			jsonObject.put("field", fe.getField());
			jsonObject.put("message", fe.getDefaultMessage());
			jsonArray.put(jsonObject);
		}
		map.put("messages", jsonArray.toString());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map); // 400
	}



}
