import java.io.*;
import java.net.http.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;


public class App {
    public static void main(String[] args) {
        Monitoring monitoring = new Monitoring();
        monitoring.getNews(System.getenv("KEYWORD"), 5, 1, SortType.sim);
    }
}

enum SortType {
    sim("sim"), date("date");
    final String value;
    SortType(String value) {
        this.value = value;
    }
}

class Monitoring {
    private final Logger logger;
    private final static String CLIENT_ID = System.getenv("NAVER-CLIENT-ID");
    private final static String CLIENT_SECRET = System.getenv("NAVER-CLIENT-SECRET");

    public Monitoring() {
//        logger = Logger.getLogger("Monitoring");
        logger = Logger.getLogger(Monitoring.class.getName());
        logger.setLevel(Level.SEVERE);
        logger.info("Monitoring 객체 생성");
    }

    public void getNews(String keyword, int display, int start, SortType sort) {

        try {
            String response = getDataFromAPI("news.json", keyword, display, start, sort);


            String[] tmp = response.split("title\":\"");
            String[] result = new String[display];
            for (int i = 1; i < display; i++) {     //첫 title 이전 문자열 필요없기에
                result[i-1] = tmp[i].split("\",")[0];
            }
            for (String s : result)
                logger.info(s);

            File file = new File("%d_%s.txt".formatted(new Date().getTime(), keyword));

            if (!file.exists()) {
                logger.info(file.createNewFile() ? "신규 생성" : "이미 있음");
            }

            try (FileWriter fw = new FileWriter(file)) {
                for (String line : result) {
                    fw.write(line + "\n");
                }
                logger.info("기록 성공");
            }
            logger.info("제목 목록 생성 완료");


            String imageResponse = getDataFromAPI("image", keyword, display, start, SortType.sim);
            String imageLink = imageResponse
                    .split("link\":\"")[1].split("\",")[0]
                    .split("\\?")[0]
                    .replace("\\", "");
            logger.info(imageLink);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageLink))
                    .build();

            String[] tmp2 = imageLink.split("\\.");
            Path path = Path.of("%d_%s.%s".formatted(
                    new Date().getTime(), keyword, tmp2[tmp2.length-1]
            ));
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofFile(path));

        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    public String getDataFromAPI(String path, String keyword, int display, int start, SortType sort) {
        String url = "https://openapi.naver.com/v1/search/%s".formatted(path);
        String params = "query=%s&display=%d&start=%d&sort=%s"
                .formatted(
                        keyword, display, start, sort.value
                );
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "?" + params))
                .GET()
                .header("X-Naver-Client-Id", CLIENT_ID)
                .header("X-Naver-Client-Secret", CLIENT_SECRET)
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info(Integer.toString(response.statusCode()));
            logger.info(response.body());
            return response.body();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }

        return "";
    }
}
