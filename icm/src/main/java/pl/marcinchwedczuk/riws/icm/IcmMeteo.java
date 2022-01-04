package pl.marcinchwedczuk.riws.icm;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcmMeteo {
    public byte[] getMeteoImage() throws IOException {
        Connection.Response doc = Jsoup.connect("https://www.meteo.pl/").execute();
        // Save cookies for later
        Map<String, String> cookies = doc.cookies();
        cookies.forEach((key, value) -> System.out.printf("COOKIE: %s = %s%n", key, value));

        // _uniqid={number} parameter
        //  obj.parameters[encodeURIComponent(obj.uniqueParameter)] =
        //            new Date().getTime().toString().substr(5) + Math.floor(Math.random() * 100).toString();
        String uniqid = Long.toString(System.currentTimeMillis()).substring(5) +
                Integer.toString((int)(ThreadLocalRandom.current().nextDouble() * 100));
        Connection.Response page1 = Jsoup.connect("https://www.meteo.pl/meteorogram_um.php?_uniqid=" + uniqid)
                .cookies(cookies)
                .execute();

        // System.out.println(page1.body());
        cookies = page1.cookies();

        Document doc1 = page1.parse();
        String coords = null;
        for (Element cityLink : doc1.select("div.lista_A")) {
            System.out.println("LINK: " + cityLink.toString());
            if (cityLink.text().contains("Warsaw") || cityLink.text().contains("Warszawa")) {
                coords = cityLink.attr("onClick")
                        .replaceAll("\s+", "")
                        .replace("showMgram(", "")
                        .replace(")", "");
                break;
            }
        }

        System.out.println("coords = " + coords);

        // Grap iFrame with time
        Connection.Response page2 = Jsoup.connect("https://www.meteo.pl/um/ramka_um_city_pl.php")
                .cookies(cookies)
                .execute();

        cookies = page2.cookies();

        String startTime = null;
        Pattern p = Pattern.compile("var UM_SST=\"(?<startTime>[^\"]+)\"");
        Matcher m = p.matcher(page2.body());
        if (m.find()) {
            startTime = m.group("startTime");
            System.out.println("Start Time: " + startTime);
        } else {
            throw new RuntimeException("Missing start time!");
        }

        /*
        function showMgram(name,x,y,cname) {
        document.getElementById(name).contentWindow.show_mgram_list_all("",x,y,lang,cname);
        };
         */
        /*

<script language='JavaScript'>var UM_YYYY=2022;var UM_MM=1;var UM_DD=4;var UM_ST=12;var UM_SYYYY="2022";var UM_SMM="01";var UM_SDD="04";var UM_SST="12";</script>
<SCRIPT LANGUAGE="JavaScript">

var Year=UM_SYYYY;
var Month=UM_SMM;
var Day=UM_SDD;
var Start_time=UM_SST;
var KON= top.KON;
         */

        // nazwa_strony="/um/php/meteorogram_list.php?ntype=0u&fdate="+Year+Month+Day+Start_time+"&row="+mgy+"&col="+mgx+"&lang="+lang+"&cname="+cname;
        //
        //	show_kwadrA(mgx,mgy);

        // "showMgram(1641322385, 250, 406 ,'Warszawa')
        String[] params2 = coords.split(",");
        LocalDate now = LocalDate.now();
        String dd = String.format("%04d%02d%02d%s", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), startTime);
        String url = "/um/php/meteorogram_list.php?ntype=0u&fdate="+dd+
                "&row="+params2[2]+"&col="+params2[1]+"&lang="+"pl"+"&cname="+params2[3].replaceAll("'", "");

        // /um/php/meteorogram_list.php?ntype=0u&fdate=2022010412&row=406&col=250&lang=pl&cname=Warszawa
        // /um/php/meteorogram_list.php?ntype=0u&fdate=20221412&row=250&col=406&lang=pl&cname='Warszawa'

        String src = null;
        for (int i = 0; i < 60; i++) {
            System.out.println("URL: " + url);
            Document imgFrame = Jsoup.connect("https://www.meteo.pl/" + url)
                    .cookies(cookies)
                    .get();

            System.out.println(imgFrame.outerHtml());

            if (imgFrame.select("#meteorogram").get(0) != null) break;
            try { Thread.sleep(1000); } catch (Exception e) { }
        }

        url = "/um/metco/mgram_pict.php?ntype=0u&fdate="+dd+"&row="+params2[2]+"&col="+params2[1]+"&lang=pl";

        Connection.Response resultImageResponse = Jsoup.connect("https://www.meteo.pl/" + url)
                .cookies(cookies)
                .ignoreContentType(true)
                .execute();

        return resultImageResponse.bodyAsBytes();
    }
}
