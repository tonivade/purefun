/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public enum HttpStatus {
  
  OK(200),
  CREATED(201),
  ACCEPTED(202),
  NO_CONTENT(204),
  PARTIAL_CONTENT(206),
  
  MOVED_PERMANENTLY(301),
  MOVED_TEMPORARILY(302),

  BAD_REQUEST(400), 
  UNAUTHORIZED(401), 
  FORBIDDEN(403), 
  NOT_FOUND(404), 
  METHOD_NOT_ALLOWED(405), 
  PROXY_AUTHENTICATION_REQUIRED(407), 
  REQUEST_TIMEOUT(408), 
  UNSUPPORTED_MEDIA_TYPE(415), 
  
  INTERNAL_SERVER_ERROR(500),
  NOT_IMPLEMENTED(501),
  BAD_GATEWAY(502),
  SERVICE_UNAVAILABLE(503),
  GATEWAY_TIMEOUT(504);
  
  private final int code;
  
  HttpStatus(int code) {
    this.code = code;
  }
  
  public int code() {
    return code;
  }

  public static HttpStatus fromCode(int code) {
    for (HttpStatus httpStatus : values()) {
      if (httpStatus.code == code) {
        return httpStatus;
      }
    }
    throw new IllegalArgumentException("invalid code: " + code);
  }
}
