package pl.marcinchwedczuk.riws.icm;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcmMeteo {
    private final String baseUrl = "https://www.meteo.pl";
    private final Map<String, String> cookieStore = new HashMap<>();

    public byte[] getMeteoImage() throws IOException {
        // Just call to grab cookies
        mkConnection("/").execute();

        Document page1 = mkConnection("/meteorogram_um.php?_uniqid=%s", mkUniqId())
                .get();

        Optional<String> maybeMeteoParams = page1.select("div.lista_A").stream()
                .filter(cityLink -> cityLink.text().contains("Warsaw") || cityLink.text().contains("Warszawa"))
                .findFirst()
                .map(cityLink -> cityLink.attr("onClick")
                        .replaceAll("\s+", "")
                        .replace("showMgram(", "")
                        .replace(")", ""));

        if (maybeMeteoParams.isEmpty())
            return null;

        // Grab iFrame with time
        Connection.Response page2 = mkConnection("/um/ramka_um_city_pl.php")
                .execute();

        String startTime = null;
        Pattern p = Pattern.compile("var UM_SST=\"(?<startTime>[^\"]+)\"");
        Matcher m = p.matcher(page2.body());
        if (m.find()) {
            startTime = m.group("startTime");
        } else {
            return null;
        }

        // split: 1641322385,250,406,'Warszawa'
        String[] meteoParams = maybeMeteoParams.get().split(",");
        LocalDate now = LocalDate.now();
        String currentDay = String.format("%04d%02d%02d%s", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), startTime);
        String url = "/um/php/meteorogram_list.php?ntype=0u&fdate="+currentDay+
                "&row="+meteoParams[2]+"&col="+meteoParams[1]+"&lang=pl&cname="+meteoParams[3].replaceAll("'", "");

        // Wait for meteogram - we need to refresh page several times
        for (int i = 0; i < 60; i++) {
            Document imgFrame = mkConnection(url).get();
            if (imgFrame.select("#meteorogram").get(0) != null)
                break;

            try { Thread.sleep(1000); } catch (Exception e) { }
        }

        // Finally grab image
        url = "/um/metco/mgram_pict.php?ntype=0u&fdate="+currentDay+"&row="+meteoParams[2]+"&col="+meteoParams[1]+"&lang=pl";

        Connection.Response resultImageResponse = mkConnection(url)
                .ignoreContentType(true)
                .execute();

        return resultImageResponse.bodyAsBytes();
    }

    private String mkUniqId() {
        return Long.toString(System.currentTimeMillis()).substring(5) +
                Integer.toString((int) (ThreadLocalRandom.current().nextDouble() * 100));
    }

    private Connection mkConnection(String relativeUrlFormat, Object... params) {
        if (!relativeUrlFormat.startsWith("/")) {
            relativeUrlFormat  = "/" + relativeUrlFormat;
        }

        String relativeUrl = params.length == 0
                ? relativeUrlFormat
                : String.format(relativeUrlFormat, params);

        return Jsoup.connect(baseUrl + relativeUrl)
                .cookies(cookieStore);
    }
}
