import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.build.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubGuesser {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args){
        System.out.println("Hello World");

        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Jackson works!");

        String owner = "MegaModular";
        String repo = "Game-Gam-2022";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo))
                .header("Accept", "application/vnd.github+json")
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status code: " + response.statusCode());
            System.out.println("Response body:");
            System.out.println(response.body());

            SimpleRepo repo1 = mapper.readValue(response.body(), SimpleRepo.class);
            System.out.println(repo1.name);

        }catch (Exception e) {
            System.out.println("Exception = " + e.getMessage());
        }


    }
}
