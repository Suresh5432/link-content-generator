package com.linklens.extractionservice.websiteanalysis.controller;

import com.linklens.extractionservice.websiteanalysis.service.WebsiteAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalWebsiteAnalysisReportController.class)
class InternalWebsiteAnalysisReportControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WebsiteAnalysisService service;

    @Test
    void rejectsAnonymousInternalReportAccess() throws Exception {
        mockMvc.perform(get("/internal/v1/website-analyses/00000000-0000-0000-0000-000000000001/report")
                        .queryParam("userId", "some-user"))
                .andExpect(status().isUnauthorized());
    }
}
