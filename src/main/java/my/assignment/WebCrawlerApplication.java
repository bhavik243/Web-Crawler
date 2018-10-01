package my.assignment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bkothari on 25/09/18.
 */
@Slf4j
@SpringBootApplication
public class WebCrawlerApplication extends SpringBootServletInitializer {

    private static Queue<String> unmarkedUrls = new LinkedList<>();
    private static Set<String> markedUrls = new HashSet<>();

    private static final String DIRECT_LINKS_REG_EX = "http[s]*://www.prudential.co.uk/([-a-zA-Z0-9~.@#%\\//]+)*";
    private static final String IMAGE_REG_EX = "<img src[ ]*=[ ]*\"([-a-zA-Z0-9~\\//&;=?.]+)*\"";
    private static final String MAIN_URL_REG_EX = "<a [target=\"_blank\" ]*href[ ]*=[ ]*\"([-a-zA-Z0-9~.\\//]+)*\">";
    private static final String BACKWARD_SLASH = "\"";
    private static final String FORWARD_SLASH = "/";

    public static void main(String[] args) {
        SpringApplication.run(WebCrawlerApplication.class, args);

        crawlNow("https://www.prudential.co.uk/");
        showCrawledResults();
    }

    /**
     * Crawl the website and add to the list of unmarkedUrls which would be considered for crawling and same url to be added to marked when it would be
     * @param startingUrl
     * @throws IOException
     */
    private static void crawlNow(String startingUrl) {
        String hostname = startingUrl;
        unmarkedUrls.add(startingUrl);
        markedUrls.add(startingUrl);

        try {
            while(!unmarkedUrls.isEmpty()) {
                String crawledUrl = unmarkedUrls.poll();
                log.info("Crawled URL: {}", crawledUrl);

                String content = fetchContentFromWebsiteToBeCrawled(crawledUrl);
                searchWebsiteInTheGivenContent(content, MAIN_URL_REG_EX, hostname);
                searchWebsiteInTheGivenContent(content, IMAGE_REG_EX, hostname);
                searchWebsiteInTheGivenContent(content, DIRECT_LINKS_REG_EX, hostname);
            }
        } catch(IOException e) {
            log.error("Something went wrong while crawling website Urls: {}", e.getMessage(), e);
        }

    }

    /**
     * Fetch content from the website to be crawled
     * @param crawledUrl
     * @return String - content of the website.
     * @throws IOException
     */
    private static String fetchContentFromWebsiteToBeCrawled(final String crawledUrl) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(crawledUrl);
            InputStream is = url.openConnection().getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(is));
        } catch(MalformedURLException e) {
            log.error("Malformed URL : {}", crawledUrl);
            log.error("Exception message : {}", e.getMessage(), e);
        } catch(IOException e) {
            log.error("IOException for URL : {}", crawledUrl);
            log.error("Exception message : {}", e.getMessage(), e);
        }

        StringBuilder stringBuilder = new StringBuilder();
        String content = null;
        while(bufferedReader!= null && (content = bufferedReader.readLine()) != null) {
            stringBuilder.append(content);
        }
        return stringBuilder.toString();
    }

    /**
     * Search website URLs to be crawled and not to be crawled both from the given content.
     * @param content
     * @param regex
     * @param hostname
     */
    private static void searchWebsiteInTheGivenContent(final String content, final String regex, final String hostname) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while(matcher.find()) {
            String website = matcher.group();

            if(website.contains("<a href") || website.contains("img src")) {
                website = website.substring(website.indexOf(BACKWARD_SLASH));
                if (website.startsWith(hostname)) {
                    website = website.substring(0, website.lastIndexOf(BACKWARD_SLASH));
                } else {
                    website = hostname + website.substring(website.indexOf(FORWARD_SLASH) + 1, website.lastIndexOf(BACKWARD_SLASH));
                }
            }

            if(!markedUrls.contains(website)) {
                if(website.startsWith(hostname) && !(website.contains(".pdf") || website.contains(".jpg")
                        || website.contains(".gif") || website.contains(".png"))) {
                    unmarkedUrls.add(website);
                }
                markedUrls.add(website);
                log.info("Website added for crawling : {}", website);
            }
        }
        log.info("Count MarkedUrls : {}", markedUrls.size());
    }

    private static void showCrawledResults() {
        log.info("Results: ");
        log.info("Websites crawled: {}", markedUrls.size());

        for(String url : markedUrls) {
            log.info("URLs crawled: {}", url);
        }
    }

}
