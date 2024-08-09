package org.example;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;

public class ElementFinder {
    private ElementFinder() {
    }

    public static Elements getSchemas(final Document mainPage) {
        final Elements findingArea = mainPage.select("div#reg_all_sections");
        if (findingArea.isEmpty()) {
            throw new RuntimeException("Не получилось найти область со ссылками на главной странице");
        }
        final Element areaObject = findingArea.get(0);
        final Element areaChildObject = areaObject.child(0);
        final Element listWithPageUrls = areaChildObject.child(5);
        return listWithPageUrls.children();
    }

    public static String getPageHref(final Element element){
        return element.select("a")
            .get(0)
            .attr("href");
    }

    public static List<String> getFileDownloadUrls(final Document page) {
        final List<String> fileDownloadUrls = new LinkedList<>();

        final Elements tableData = page.select("table.MsoNormalTable")
            .get(0)
            .children();
        for (Element tableDatum : tableData) {
            if (tableDatum.children().size() != 4) {
                continue;
            }

            final Elements a = tableDatum.child(2)
                .select("a");
            if (a.isEmpty()) {
                continue;
            }

            fileDownloadUrls.add(a.get(0)
                .attr("href"));
        }
        return fileDownloadUrls;
    }
}
