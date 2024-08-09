package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.example.Constants.*;
import static org.example.ElementFinder.*;

public class Main {
    private static final List<String> MAIN_PAGE_URLS = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Начинаем процесс загрузки файлов.");
        final Document mainPage = Jsoup.connect(MAIN_URL)
            .userAgent(USER_AGENT)
            .referrer(REFERRER)
            .get();
        System.out.println("Успешное подключение на главную страницу");

        final Elements listSchemas = getSchemas(mainPage);
        System.out.printf("Найдено %d записей для поиска файлов%n", listSchemas.size());

        for (int i = 0; i < listSchemas.size(); i++) {
            MAIN_PAGE_URLS.add(getPageHref(listSchemas.get(i)));
        }
        System.out.printf("Найдено %d страниц для поиска файлов%n", MAIN_PAGE_URLS.size());

        for (final String pageUrl : MAIN_PAGE_URLS) {
            final Document page = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .get();
            final List<String> fileDownloadUrls = getFileDownloadUrls(page);
            //todo download here
        }
    }
}
