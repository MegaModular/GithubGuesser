import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.build.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class GithubGuesser {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random rng = new Random();

    private static final Set<String> codeFileExtensions = Set.of(".gd", ".java", ".js", ".py", ".c", ".cpp");

    public static String codeFileURL = null;

    public static void main(String[] args){

        String owner = "MegaModular";
        String repo = "Game-Gam-2022";

        SimpleRepo r1 = sendRepoRequest(owner, repo);

        /*
        try {
            response = sendContentRequest(owner, repo, null);
            GithubContent[] contents = mapper.readValue(response.body(), GithubContent[].class);
            GithubContent.printContentArray(contents);
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
        System.out.println(response.body());
*/
        List<String> s = createFileListFromRepo(owner, repo);

        System.out.println(s);
        System.out.println(codeFileURL);

    }

    //Gets repo (Name, Description, etc.)
    public static SimpleRepo sendRepoRequest(String owner, String repo){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo))
                    .header("Accept", "application/vnd.github+json").build();

            System.out.println("Sending Request to " + request.uri());

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());

            SimpleRepo repo1 = mapper.readValue(response.body(), SimpleRepo.class);

            System.out.println("Response Recieved");
            return repo1;

        }catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    //Gets Repo Content
    public static HttpResponse<String> sendContentRequest(String owner, String repo, String link){
        String urlToSend = "https://api.github.com/repos/" + owner + "/" + repo + "/";
        urlToSend += "contents";

        if (!link.isEmpty()) {
            urlToSend = link;
        }

        System.out.println("Link = ");
        System.out.println(link);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlToSend))
                .header("Accept", "application/vnd.github+json").build();

        System.out.println("Sending Request to " + request.uri());

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Error in sendContentRequest method, ");
            System.out.println(e.getMessage());
            return null;
        }
    }


    //Recursively finds all code files and sets a codeFileURL.
    public static List<String> createFileListFromRepo(String owner, String repo) {
        List<String> returnList = new ArrayList<>();
        Stack<String> pathsToQuery = new Stack<>();

        try {
            //initial search
            HttpResponse<String> response = sendContentRequest(owner, repo, "");
            GithubContent[] contents = mapper.readValue(response.body(), GithubContent[].class);
            do {
                //Loop through content
                for (GithubContent c : contents) {
                    if (c.type.equals("dir")) {
                        pathsToQuery.push(c.self);
                    }
                    else if (codeFileExtensions.stream().anyMatch(c.name::contains)) {

                        returnList.add(c.name);
                        if (codeFileURL == null) {
                            codeFileURL = c.self;
                        }
                        if (rng.nextInt() % 5 == 0) {
                            codeFileURL = c.self;
                        }
                    }
                }
                //if must search deeper, then do so.
                if (!pathsToQuery.isEmpty()) {
                    response = sendContentRequest(owner, repo, pathsToQuery.pop());
                    contents = mapper.readValue(response.body(), GithubContent[].class);
                }
            }while(!pathsToQuery.isEmpty());
        } catch (Exception e) {
            System.out.println("Error in createFileList");
            System.out.println(e.getMessage());
        }
        return returnList;
    }
}
