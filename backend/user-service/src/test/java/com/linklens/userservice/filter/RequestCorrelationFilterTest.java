package com.linklens.userservice.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {
 @Test void preservesSafeCorrelationIdAndClearsMdcAfterRequest() throws Exception {
  var request=new MockHttpServletRequest(); request.addHeader("X-Correlation-ID","trace-123");
  var response=new MockHttpServletResponse(); var chain=new MockFilterChain();
  new RequestCorrelationFilter().doFilter(request,response,chain);
  assertEquals("trace-123",response.getHeader("X-Correlation-ID"));
  assertNull(MDC.get("correlationId"));
 }
 @Test void replacesUnsafeCorrelationId() throws Exception {
  var request=new MockHttpServletRequest(); request.addHeader("X-Correlation-ID","bad\r\nforged");
  var response=new MockHttpServletResponse();
  new RequestCorrelationFilter().doFilter(request,response,new MockFilterChain());
  assertNotEquals("bad\r\nforged",response.getHeader("X-Correlation-ID"));
  assertNotNull(response.getHeader("X-Correlation-ID"));
 }
}
