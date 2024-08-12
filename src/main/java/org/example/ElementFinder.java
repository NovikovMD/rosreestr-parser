package org.example;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;

import static org.example.Constants.ROOT_URL;

public class ElementFinder {
    private ElementFinder() {
    }

    public static Elements getSchemas(final Document mainPage) {
        final Element areaObject = mainPage.select("div#reg_all_sections")
            .get(0);
        final Element areaChildObject = areaObject.select("div[data-iblock-id='20']")
            .get(0);
        final Element listWithPageUrls = areaChildObject.select("ul")
            .get(0);
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
            fileDownloadUrls.add(a.get(0)
                .attr("href"));
        }
        return fileDownloadUrls;
    }
}
