package com.example.springGroupBA.entity.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

// API응답에 사용된다. 클라이언트에게 특정메세지와 함께 결과 데이터를 전달할때 활용한다.
// 특정메소드와 함께 결과 데이터를 전달할때 활용된다.
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KakaoMsg {

  private String msg;
  private Object result;

  public KakaoMsg(String msg, Object result) { // msg는 success를, 객체 result는 kakaoInfo를 사용했었다.
    this.msg = msg;
    this.result = result;
  }
}
