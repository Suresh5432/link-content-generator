package com.linklens.extractionservice.service;

import com.linklens.extractionservice.dto.ExtractedPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ContentExtractor {

    private static final int MAX_TITLE_LENGTH=1000;
    private static final int MAX_DESCRIPTION_LENGTH=3000;
    private static final int MAX_TEXT_LENGTH=500_000;

    private static final Pattern WHITESPACE= Pattern.compile("\\s+");

    private static final Set<String> REMOVE_SELECTORS=Set.of(
            "script",
            "style",
            "noscript",
            "svg",
            "canvas",
            "iframe",
            "nav",
            "footer",
            "header",
            "aside",
            "form",
            "button",
            "input",
            "select",
            "textarea"
    );

    public ExtractedPage extract(SafeWebFetcher.FetchedPage fetchedPage) {
        Document document = null;
        try {
            document = Jsoup.parse(
                    new String(fetchedPage.body(), detectCharset(fetchedPage)),
                    fetchedPage.finalUrl()
            );
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable to parse the webpage content using jsoup");
        }

        String title=extractTitle(document);
        String description=extractDescription(document);
        String canonicalUrl=extractCanonicalUrl(document);
        String language=extractLanguage(document);

        String extractedText=extractMainContent(document);

        if (extractedText.isBlank()){
            throw new IllegalArgumentException("Unable to extract readable webpage content");
        }

        int wordCount=countWords(extractedText);

        String contentType=detectContentType(
                document,
                extractedText
        );

        return ExtractedPage.builder()
                .title(title)
                .description(description)
                .canonicalUrl(canonicalUrl)
                .contentType(contentType)
                .language(language)
                .extractedText(extractedText)
                .build();
    }

    private String extractTitle(Document document){

        String title=firstNonBlank(
                getMetaContent(document ,"og:title"),
                getMetaContent(document, "twitter:title"),
                document.title()
        );

        return limit(cleanText(title),MAX_TITLE_LENGTH);
    }

    private String extractDescription(Document document){

        String description=firstNonBlank(
                    getMetaContent(document, "description"),
                getMetaContent(document, "og:description"),
                getMetaContent(document, "twitter:description")
        );

        return  limit(cleanText(description), MAX_DESCRIPTION_LENGTH);
    }

    private String extractCanonicalUrl(Document document){
        Element canonical=document.selectFirst(
                "link[rel=canonical][href]"
        );

        if(canonical==null){
            return null;
        }
        String href=canonical.attr("href").trim();

        if (href.isBlank()){
            return null;
        }

        try{
            URI resloved =URI.create(document.location()).resolve(href);

            if (!"http".equalsIgnoreCase(resloved.getScheme())&&
                    !"https".equalsIgnoreCase(resloved.getScheme())){
                return null;
            }
            if (resloved.getHost()==null||
                resloved.getRawUserInfo()!=null){
                return null;
            }

            return resloved.toString();
        }
        catch (IllegalArgumentException e){
            return null;
        }
    }

    private String extractLanguage(Document document){
        Element html=document.selectFirst("html");

        if(html==null){
            return null;
        }
        String language=html.attr("lang").trim();

        if (language.isBlank()){
            return null;
        }

        return limit(language,20);
    }

    private String extractMainContent(Document document){

        for(String selector:REMOVE_SELECTORS){
            document.select(selector).remove();
        }

        document.select( "[class*=advert], [id*=advert], " +
                "[class*=cookie], [id*=cookie], " +
                "[class*=social], [id*=social], " +
                "[class*=sidebar], [id*=sidebar], " +
                "[class*=popup], [id*=popup], " +
                "[class*=newsletter], [id*=newsletter]"
        ).remove();

        Element main=document.selectFirst("article, main, [role=main]");

        String text;

        if (main!=null){
            text = main.text();
        }
        else {
            Element body=document.body();
            text= body.text();
        }

        text=cleanText(text);

        return limit(text,MAX_TEXT_LENGTH);
    }

    private String detectContentType(Document document, String extractedText){
        String ogType=getMetaContent(document, "og:type");

        if (ogType!=null){
            String normalized= ogType.toLowerCase(Locale.ROOT);

            if (normalized.contains("article")){
                return "ARTICLE";
            }
            if (normalized.contains("product")){
                return "PRODUCT";
            }
            if (normalized.contains("video")){
                return "VIDEO";
            }
        }

        if (document.selectFirst("article")!=null){
            return "ARTICLE";
        }

        if (document.selectFirst("pre code, .highlight, .codehilite")!=null){
            return "DOCUMENTATION";
        }

        if (document.selectFirst("[itemtype*=schema.org/product]")!=null){
            return "PRODUCT";
        }

        if (extractedText.length()<500){
            return "GENERAL";
        }
        return "WEBPAGE";
    }

    private String getMetaContent(Document document, String name){

        Element element=document
                .selectFirst("meta[name="+name+"], "+
                        "meta[property="+name+"]"
                );
        if (element==null){
            return null;
        }
        return element.attr("content").trim();
    }

    private String firstNonBlank(String... values){
        for (String value:values){
            if (value!=null&&!value.isBlank()){
                return value;
            }
        }
        return null;
    }

    private String cleanText(String text){

        if (text==null){
            return null;
        }

        return WHITESPACE.matcher(text)
                .replaceAll(" ")
                .trim();
    }

    private String limit(String text,int max){
        if (text==null ||text.length()<=max){
            return text;
        }
        return text.substring(0, max);
    }

    private int countWords(String text){
        if (text==null ||text.isBlank()){
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private String detectCharset(SafeWebFetcher.FetchedPage fetchedPage){
        return "UTF-8";
    }
}
