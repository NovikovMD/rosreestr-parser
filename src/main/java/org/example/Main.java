package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import static org.example.Constants.*;
import static org.example.ElementFinder.*;

public class Main {
    private static final List<String> MAIN_PAGE_URLS = new LinkedList<>();
    private static long counter = 0;

    public static void main(String[] args) throws Exception {
        trustAllCertificates();

        System.out.println("Начинаем процесс парсинга файлов.");
        final Document mainPage = Jsoup.connect(MAIN_URL)
            .userAgent(USER_AGENT)
            .referrer(REFERRER)
            .get();
        System.out.println("Успешное подключение на главную страницу");

        final Elements listSchemas = getSchemas(mainPage);
        for (int i = 0; i < listSchemas.size(); i++) {
            MAIN_PAGE_URLS.add(getPageHref(listSchemas.get(i)));
        }
        System.out.printf("Найдено %d страниц для поиска файлов%n", MAIN_PAGE_URLS.size());

        for (final String pageUrl : MAIN_PAGE_URLS) {
            final Document page = Jsoup.connect(ROOT_URL + pageUrl)
                .userAgent(USER_AGENT)
                .get();
            final List<String> fileDownloadUrls = getFileDownloadUrls(page);
            downloadFiles(fileDownloadUrls);
            counter += fileDownloadUrls.size();
        }
        System.out.println("Было загружено " + counter + " файлов");
    }

    private static void downloadFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            try {
                String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                System.out.printf("Начало загрузки файла %s%n", fileName);
                downloadFile(fileUrl, fileName);
                System.out.printf("Файл %s успешно загружен%n", fileName);
            } catch (IOException e) {
                System.err.printf("Ошибка при загрузке файла: %s%n", fileUrl);
                e.printStackTrace();
            }
        }
    }

    private static void downloadFile(String fileUrl, String fileName) throws IOException {
        // Ensure the file URL is absolute
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            fileUrl = ROOT_URL + fileUrl;
        }

        // Open connection and set headers
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/octet-stream");
        connection.setRequestProperty("Referer", REFERRER);
        connection.setRequestMethod("GET");

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Print response code and message for debugging
            System.err.printf("Server returned HTTP response code: %d for URL: %s%n",
                connection.getResponseCode(), fileUrl);
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
    }

    private static void trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Also, ignore hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}
