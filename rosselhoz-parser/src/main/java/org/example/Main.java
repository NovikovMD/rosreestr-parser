package org.example;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.example.Configuration.trustAllCertificates;
import static org.example.Constants.REFERRER;
import static org.example.Constants.ROOT_URL;
import static org.example.Constants.USER_AGENT;
import static org.example.ElementFinder.getDataTables;
import static org.example.ElementFinder.getDownloadUrls;

public class Main {
    public static void main(String[] args) throws Exception {
        final SSLConnectionSocketFactory socketFactory = trustAllCertificates();

        final List<Element> dataTables;
        try {
            dataTables = getDataTables();
        } catch (IOException exception) {
            System.err.println("Ошибка при получении данных с главной страницы");
            throw exception;
        }

        final List<String> fileDownloadUrls = dataTables.stream()
            .flatMap(table -> getDownloadUrls(table).stream())
            .toList();
        downloadFiles(fileDownloadUrls, socketFactory);
        System.out.println("Было загружено " + fileDownloadUrls.size() + " файлов");
    }

    private static void downloadFiles(List<String> fileUrls, SSLConnectionSocketFactory socketFactory) throws Exception {
        for (final String fileUrl : fileUrls) {
            final String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            System.out.printf("Начало загрузки файла %s%n", fileName);
            ExceptionRepeater.aroundInvoke(() -> {
                try {
                    downloadFile(fileUrl, fileName, socketFactory);
                    System.out.printf("Файл %s успешно загружен%n", fileName);
                } catch (IOException e) {
                    System.err.printf("Ошибка при загрузке файла: %s%n", fileUrl);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void downloadFile(String fileUrl, String defaultFileName, SSLConnectionSocketFactory socketFactory) throws IOException {
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            fileUrl = ROOT_URL + fileUrl.trim().replace(" ", "%20");
        }
        Path path = null;
        try (CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)
            .build()) {

            final HttpGet request = new HttpGet(fileUrl);
            request.addHeader("User-Agent", USER_AGENT);
            request.addHeader("Accept", "application/octet-stream");
            request.addHeader("Referer", REFERRER);

            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                final HttpEntity entity = response.getEntity();
                final String fileName = getFileName(fileUrl, response, defaultFileName);

                try (final InputStream in = entity.getContent()) {
                    path = Paths.get(fileName.replace("%2F", "_"));
                    Files.copy(
                        in,
                        path,
                        REPLACE_EXISTING);
                }
            }
        }

        if (path.getFileName().toString().toLowerCase().endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(path.toFile())) {
                Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
                while (entries.hasMoreElements()) {
                    ZipArchiveEntry entry = entries.nextElement();
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
