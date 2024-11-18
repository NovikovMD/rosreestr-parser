package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.example.Constants.MAIN_URL;
import static org.example.Constants.REFERRER;
import static org.example.Constants.USER_AGENT;

public class ElementFinder {
    private ElementFinder() {
    }

    public static String getPageHref(final Element element) {
        return element.select("a")
            .get(0)
            .attr("href");
    }

    public static List<String> getDownloadUrls(final Element dataTable) {
        final Elements trElements = dataTable.select("tbody")
            .get(0)
            .children();

        return range(0, trElements.size())
            .dropWhile(index -> trElements.get(index).children().size() == 1) // пропускаем лидирующие заголовки
            .filter(index -> trElements.get(index).children().size() == 1)
            .mapToObj(index -> trElements.get(index - 1))
            .filter(trElement -> trElement.children().size() != 1)
            .map(trElement -> {
                final Element firstChild = trElement.child(0);
                return getPageHref(firstChild);
            })
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    final Element lastElement = trElements.get(trElements.size() - 1);
                    list.add(getPageHref(lastElement.child(0)));
                    return list;
                }));
    }

    /**
     * Подключение на главную страницу и получение таблиц collapse элементов
     */
    public static List<Element> getDataTables() throws IOException {
        System.out.println("Начинаем процесс парсинга файлов.");
        final Document mainPage = Jsoup.connect(MAIN_URL)
            .userAgent(USER_AGENT)
            .referrer(REFERRER)
            .get();
        System.out.println("Успешное подключение на главную страницу");

        final Element mainArea = mainPage.select("div.publication-list.list-collapse")
            .get(0);
        final Elements collapsedItems = mainArea.children();
        final List<Element> tables = collapsedItems.stream()
            .map(item -> {
                final Element collapseElement = item.select("div.wrap-more")
                    .get(0);
                final Element collapsedTable = collapseElement.select("table")
                    .get(0);
                return collapsedTable;
            })
            .toList();

        System.out.printf("Найдено %d таблиц для поиска файлов%n", tables.size());
        return tables;
    }
}
