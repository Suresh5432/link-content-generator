package com.linklens.summarizationservice.websiteanalysis.controller;

import com.linklens.summarizationservice.websiteanalysis.WebsiteAnalysisReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebsiteAnalysisReportController.class)
class WebsiteAnalysisReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WebsiteAnalysisReportService service;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/website-analysis-reports"))
                .andExpect(status().isForbidden());
    }
}
