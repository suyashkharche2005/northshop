package com.cart.ecom_proj.exception;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.Instant;
import java.util.*;
@RestControllerAdvice
public class ApiErrorHandler {
 @ExceptionHandler(ApiException.class) public ResponseEntity<Map<String,Object>> api(ApiException e){return error(e.getStatus(),e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){return error(HttpStatus.BAD_REQUEST,e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).distinct().toList().toString());}
 @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<Map<String,Object>> size(MaxUploadSizeExceededException e){return error(HttpStatus.BAD_REQUEST,"Image exceeds upload limit");}
 @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) public ResponseEntity<Map<String,Object>> malformed(Exception e){return error(HttpStatus.BAD_REQUEST,"Invalid request body");}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<Map<String,Object>> conflict(Exception e){return error(HttpStatus.CONFLICT,"This record is already in use or violates a uniqueness constraint");}
 private ResponseEntity<Map<String,Object>> error(HttpStatus status,String message){return ResponseEntity.status(status).body(Map.of("timestamp",Instant.now().toString(),"status",status.value(),"message",message));}
}
