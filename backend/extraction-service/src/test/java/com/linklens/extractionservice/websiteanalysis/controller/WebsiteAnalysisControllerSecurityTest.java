package com.linklens.extractionservice.websiteanalysis.controller;

import com.linklens.extractionservice.websiteanalysis.service.WebsiteAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebsiteAnalysisController.class)
class WebsiteAnalysisControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WebsiteAnalysisService websiteAnalysisService;

    @Test
    void rejectsAnonymousList() throws Exception {
        mockMvc.perform(get("/api/v1/website-analyses"))
                .andExpect(status().isUnauthorized());
    }
}
