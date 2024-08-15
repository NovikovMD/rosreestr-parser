package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.example.Configuration.trustAllCertificates;
import static org.example.Constants.*;
import static org.example.ElementFinder.getFileDownloadUrls;
import static org.example.ElementFinder.getPageUrls;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        var fact = trustAllCertificates();

        final List<String> mainPageUrls;
        try {
            mainPageUrls = getPageUrls();
        } catch (IOException exception) {
            System.err.println("Ошибка при получении данных с главной страницы");
            throw exception;
        }

        var counter = 0;
        for (String pageUrl : mainPageUrls) {
            final Document page;
            try {
                page = Jsoup.connect(ROOT_URL + pageUrl)
                    .userAgent(USER_AGENT)
                    .get();
            } catch (IOException e) {
                System.err.printf("Ошибка подключения на страницу %s%n", ROOT_URL + pageUrl);
                throw new RuntimeException(e);
            }

            final List<String> fileDownloadUrls = getFileDownloadUrls(page);
            downloadFiles(fileDownloadUrls, fact);
            counter += fileDownloadUrls.size();
        }
        System.out.println("Было загружено " + counter + " файлов");
    }

    private static void downloadFiles(List<String> fileUrls, SSLConnectionSocketFactory fact) {
        fileUrls.forEach(fileUrl -> {
            try {
                final String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                System.out.printf("Начало загрузки файла %s%n", fileName);
                downloadFile(fileUrl, fileName, fact);
                System.out.printf("Файл %s успешно загружен%n", fileName);
            } catch (IOException e) {
                System.err.printf("Ошибка при загрузке файла: %s%n", fileUrl);
                e.printStackTrace();
            }
        });
    }

    private static void downloadFile(String fileUrl, String defaultFileName, SSLConnectionSocketFactory fact) throws IOException {
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            fileUrl = ROOT_URL + fileUrl.trim();
        }
        try (CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(fact)
            .build()) {

            HttpGet request = new HttpGet(fileUrl.replace(" ", "%20"));
            request.addHeader("User-Agent", USER_AGENT);
            request.addHeader("Accept", "application/octet-stream");
            request.addHeader("Referer", REFERRER);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                String fileName = defaultFileName;

                String contentDisposition = response.getFirstHeader("Content-Disposition") != null
                    ? response.getFirstHeader("Content-Disposition").getValue() : null;

                if (contentDisposition != null) {
                    Pattern pattern = Pattern.compile("filename=\"?([^\";]*)\"?");
                    Matcher matcher = pattern.matcher(contentDisposition);
                    if (matcher.find()) {
                        fileName = matcher.group(1);
                    }
                } else {
                    URI uri = URI.create(fileUrl.replace(" ", "%20"));
                    fileName = Paths.get(uri.getPath()).getFileName().toString();
                }

                try (InputStream in = entity.getContent()) {
                    Files.copy(in, Paths.get(fileName.replace("%2", "_")), REPLACE_EXISTING);
                }
            }
        }
    }
}
