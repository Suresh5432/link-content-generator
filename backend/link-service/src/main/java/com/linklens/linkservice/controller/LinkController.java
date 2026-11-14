//package com.linklens.linkservice.controller;
//
//import com.linklens.linkservice.dto.LinkRequest;
//import com.linklens.linkservice.dto.LinkResponse;
//import com.linklens.linkservice.service.LinkService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/links")
//public class LinkController {
//
//    private final LinkService linkService;
//
//    @PostMapping
//    @ResponseStatus(HttpStatus.ACCEPTED)
//    public LinkResponse submitUrl(
//            @Valid @RequestBody LinkRequest linkRequest,
//            @AuthenticationPrincipal Jwt jwt){
//        String userId=jwt.getSubject();
//        return linkService.createLink(linkRequest,userId);
//    }
//}
