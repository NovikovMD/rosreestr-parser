package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.example.Constants.MAIN_URL;
import static org.example.Constants.REFERRER;
import static org.example.Constants.USER_AGENT;

public class ElementFinder {
    private ElementFinder() {
    }

    public static Elements getSchemas(final Document mainPage) {
        final Element areaObject = mainPage.select("div#reg_fallback_sections")
            .get(0);
        final Element listWithPageUrls = areaObject.select("ul")
            .get(0);
        return listWithPageUrls.children();
    }

    public static String getPageHref(final Element element) {
        return element.select("a")
            .get(0)
            .attr("href");
    }

    public static List<String> getFileDownloadUrls(final Document page) {
        final List<String> fileDownloadUrls = new LinkedList<>();

        final Elements select = page.select("table.MsoNormalTable");
        if (select.isEmpty()) {
            return Collections.emptyList();
        }
        final Elements tableData = select
            .get(0)
            .children()
            .get(0)
            .children();
        for (Element tableDatum : tableData) {
            var row = tableDatum.select("td");
            if (row.size() != 4) {
                continue;
            }

            final Elements lineHeader = row.get(0)
                .select("p");

            if (!lineHeader.isEmpty() && lineHeader.get(0).text().equals("4.2")) {
                fileDownloadUrls.add(row.get(1)
                    .select("a")
                    .get(0)
                    .attr("href"));
                continue;
            }
            if (!lineHeader.isEmpty() && lineHeader.get(0).text().equals("3.1")) {
                fileDownloadUrls.add(row.get(1)
                    .select("a")
                    .get(0)
                    .attr("href"));
                fileDownloadUrls.add(row.get(2)
                    .select("a")
                    .get(0)
                    .attr("href"));
                continue;
            }

            final Elements a = row.get(2)
                .select("a");
            if (a.isEmpty()) {
                continue;
            }
            fileDownloadUrls.addAll(a.stream()
                .map(el -> el.attr("href"))
                .toList());
        }
        return fileDownloadUrls;
    }


    public static List<String> getPageUrls() throws IOException {
        System.out.println("Начинаем процесс парсинга файлов.");
        final Document mainPage = Jsoup.connect(MAIN_URL)
            .userAgent(USER_AGENT)
            .referrer(REFERRER)
            .get();
        System.out.println("Успешное подключение на главную страницу");

        final Elements listSchemas = getSchemas(mainPage);
        final List<String> mainPageUrls = range(0, listSchemas.size())
            .mapToObj(index -> getPageHref(listSchemas.get(index)))
            .toList();
        System.out.printf("Найдено %d страниц для поиска файлов%n", mainPageUrls.size());
        return mainPageUrls;
    }
}
