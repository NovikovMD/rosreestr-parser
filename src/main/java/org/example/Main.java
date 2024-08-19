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
        final SSLConnectionSocketFactory socketFactory = trustAllCertificates();

        final List<String> mainPageUrls;
        try {
            mainPageUrls = getPageUrls();
        } catch (IOException exception) {
            System.err.println("Ошибка при получении данных с главной страницы");
            throw exception;
        }

        var counter = 0;
        for (final String pageUrl : mainPageUrls) {
            final Document page;
            try {
                page = Jsoup.connect(ROOT_URL + pageUrl)
                    .userAgent(USER_AGENT)
                    .get();
            } catch (IOException exception) {
                System.err.printf("Ошибка подключения на страницу %s%n", pageUrl);
                throw exception;
            }

            final List<String> fileDownloadUrls = getFileDownloadUrls(page);
            downloadFiles(fileDownloadUrls, socketFactory);
            counter += fileDownloadUrls.size();
        }
        System.out.println("Было загружено " + counter + " файлов");
    }

    private static void downloadFiles(List<String> fileUrls, SSLConnectionSocketFactory socketFactory) {
        fileUrls.forEach(fileUrl -> {
            try {
                final String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                System.out.printf("Начало загрузки файла %s%n", fileName);
                downloadFile(fileUrl, fileName, socketFactory);
                System.out.printf("Файл %s успешно загружен%n", fileName);
            } catch (IOException e) {
                System.err.printf("Ошибка при загрузке файла: %s%n", fileUrl);
                e.printStackTrace();
            }
        });
    }

    private static void downloadFile(String fileUrl, String defaultFileName, SSLConnectionSocketFactory socketFactory) throws IOException {
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            fileUrl = ROOT_URL + fileUrl.trim().replace(" ", "%20");
        }
        try (CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)
            .build()) {

            HttpGet request = new HttpGet(fileUrl);
            request.addHeader("User-Agent", USER_AGENT);
            request.addHeader("Accept", "application/octet-stream");
            request.addHeader("Referer", REFERRER);

            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                final HttpEntity entity = response.getEntity();
                final String fileName = getFileName(fileUrl, response, defaultFileName);

                try (InputStream in = entity.getContent()) {
                    Files.copy(in, Paths.get(fileName.replace("%2F", "_")), REPLACE_EXISTING);
                }
            }
        }
    }

    private static String getFileName(String fileUrl, CloseableHttpResponse response, String defaultName) {
        final String contentDisposition = response.getFirstHeader("Content-Disposition") != null
            ? response.getFirstHeader("Content-Disposition").getValue()
            : null;

        if (contentDisposition != null) {
            final Matcher matcher = Pattern.compile("filename=\"?([^\";]*)\"?").matcher(contentDisposition);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } else {
            return Paths.get(URI.create(fileUrl).getPath()).getFileName().toString();
        }
        return defaultName;
    }
}
