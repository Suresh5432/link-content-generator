package com.linklens.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID="X-Correlation-ID";


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId= UUID.randomUUID().toString();

        MDC.put("correlationId",correlationId);

        response.setHeader(CORRELATION_ID,correlationId);

        HttpServletRequestWrapper wrapper=new HttpServletRequestWrapper(request){

            @Override
            public String getHeader(String name){

                if (CORRELATION_ID.equalsIgnoreCase(name)){
                    return correlationId;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name){
                if (CORRELATION_ID.equalsIgnoreCase(name)){
                    return Collections.enumeration(
                            Collections.singletonList(correlationId)
                    );
                }
                return super.getHeaders(name);
            }
        };

        long startTime=System.currentTimeMillis();

        try{
            filterChain.doFilter(wrapper,response);
        }
        catch (Exception e){

            log.error(
                    "Request processing failed | correlationId={} | method={} | path={}",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    e
            );

            throw e;
        }
        finally{
            long duration = System.currentTimeMillis()-startTime;

            log.info(
                    "Request completed | correlationId={} | method={} | path={} | status={} | duration={}ms",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );

            MDC.remove("correlationId");
        }
    }
}
