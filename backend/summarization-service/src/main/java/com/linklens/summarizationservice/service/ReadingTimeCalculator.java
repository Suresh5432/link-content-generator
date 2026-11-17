package com.linklens.summarizationservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReadingTimeCalculator {

    @Value("${summarization.reading.words-per-minite}")
    private  int wordPerMinute;

    public int calculate(String content){

        if(content==null || content.isBlank()){
            return 1;
        }

        int wordCount=content.trim().split("\\s+").length;

        return Math.max(1,(int)Math.ceil((double)wordCount/wordPerMinute));
    }

}
