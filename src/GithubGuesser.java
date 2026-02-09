import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.build.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GithubGuesser {
    private static final String token = System.getenv("GITHUB_GUESSER_TOKEN");
    private static final Scanner scan = new Scanner(System.in);

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random rng = new Random();

    private static final Set<String> codeFileExtensions = Set.of(
            ".java", ".kt", ".kts",
            ".py",
            ".js", ".jsx", ".ts", ".tsx",
            ".c", ".h", ".cpp", ".hpp", ".cc",
            ".cs",
            ".go",
            ".rs",
            ".php",
            ".rb",
            ".swift",
            ".m", ".mm",
            ".gd",
            ".lua",
            ".sh", ".bash",
            ".ps1",
            ".html", ".css", ".scss",
            ".sql"
    );
    private static final Set<String> doNotWantExtensions = Set.of(
            ".txt", ".md", ".rst",
            ".json", ".xml", ".yml", ".yaml", ".toml", ".ini", ".cfg",
            ".csv", ".tsv",
            ".log",
            ".lock",
            ".gradle", ".properties",
            ".cmake",
            ".iml",
            ".bat",
            ".psd", ".ai",
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg", ".ico",
            ".mp3", ".wav", ".mp4", ".mov",
            ".pdf", ".doc", ".docx",
            ".zip", ".tar", ".gz", ".rar", ".7z",
            ".exe", ".dll", ".so", ".dylib", ".class", ".jar",
            ".bin", ".dat"
    );

    public static final Boolean debug = false;

    public static String codeFileURL = null;
    public static String codeFileName = null;


    public static void main(String[] args){

        int numRepos = 5;

        String codeBody;
        String fullName;

        System.out.println("How many repositories do you want to choose between?");

        int intInput = 0;
        while (true) {

            if (scan.hasNextInt()) {
                intInput = scan.nextInt();
                break;
            } else {
                System.out.println("Thats not an integer you idiot.");
                scan.next();
            }
        }
        numRepos = intInput;

        //Query Repos
        List<SimpleRepo> repos = getRandomReposFromGithub(numRepos);

        int answer = rng.nextInt(numRepos);
        SimpleRepo correctAnswer = repos.get(answer);
        fullName = correctAnswer.full_name;
        if (debug) {
            System.out.println(fullName);
        }

        List<String> s = createFileListFromRepo(fullName, 10);

        if(debug) {
            System.out.println("Code File List:");
            System.out.println(s);

        }


        codeBody = getCodeFromURL(codeFileURL);

        /*
        System.out.println("Name = " + codeFileName);
        System.out.println(codeBody);
         */
        try {
            Files.writeString(Path.of("code.txt"), "Name = " + codeFileName);
            Files.writeString(Path.of("code.txt"), codeBody);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(generateUserOutputString(repos));

        System.out.println("Which repo does the code in the text file belong to?");


        intInput = 0;
        while (true) {

            if (scan.hasNextInt()) {
                intInput = scan.nextInt();
                break;
            } else {
                System.out.println("Thats not an integer you idiot.");
                scan.next();
            }
        }
        if (answer == intInput) {
            System.out.println("You win!");
            System.out.println("You lose.");
            if (debug) {
                System.out.println("Answer :" + answer);
                System.out.println("input:" + intInput);
            }
        }

    }

    //Gets repo (Name, Description, etc.)
    public static SimpleRepo sendRepoRequest(String owner, String repo){
        try {
            if (token == null || token.isEmpty()) {
                throw new Exception("GITHUB_GUESSER_TOKEN does not exist, you need to create a new one.");
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token).build();

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
    public static HttpResponse<String> sendContentRequest(String fullName, String link){
        String urlToSend = "https://api.github.com/repos/" + fullName + "/";
        urlToSend += "contents";



        if (!link.isEmpty()) {
            urlToSend = link;
        }

        if (debug) {
            System.out.println("Link = ");
            System.out.println(link);
        }


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlToSend))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token).build();

        System.out.println("Sending Request to " + request.uri());

        try {
            if (token == null || token.isEmpty()) {
                throw new Exception("GITHUB_GUESSER_TOKEN does not exist, you need to create a new one.");
            }
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Error in sendContentRequest method, ");
            System.out.println(e.getMessage());
            return null;
        }
    }


    //Recursively finds all code files and sets a codeFileURL.
    public static List<String> createFileListFromRepo(String fullName, int maxFileCount) {
        List<String> returnList = new ArrayList<>();
        Stack<String> pathsToQuery = new Stack<>();
        int numFiles = 0;

        try {
            //initial search
            HttpResponse<String> response = sendContentRequest(fullName, "");
            GithubContent[] contents = mapper.readValue(response.body(), GithubContent[].class);
            do {
                //Loop through content
                for (GithubContent c : contents) {
                    if (c.type.equals("dir")) {
                        pathsToQuery.push(c.self);
                    }
                    else if (doNotWantExtensions.stream().noneMatch(c.name::contains) && codeFileExtensions.stream().anyMatch(c.name::contains)) {
                        maxFileCount++;
                        returnList.add(c.name);
                        if (codeFileURL == null) {
                            codeFileURL = c.self;
                            codeFileName = c.name;
                        }
                        if (rng.nextInt() % 5 == 0) {
                            codeFileURL = c.self;
                            codeFileName = c.name;
                        }
                    }
                }
                //if must search deeper, then do so.
                if (!pathsToQuery.isEmpty()) {
                    response = sendContentRequest(fullName, pathsToQuery.pop());
                    contents = mapper.readValue(response.body(), GithubContent[].class);
                }
            }while(!pathsToQuery.isEmpty() && numFiles < maxFileCount);
        } catch (Exception e) {
            System.out.println("Error in createFileList");
            System.out.println(e.getMessage());
        }
        return returnList;
    }

    //returns the decoded body of code from specified URL
    public static String getCodeFromURL(String url) {
        HttpResponse<String> response = sendContentRequest(null, url);
        GithubContent content;
        try {
            content = mapper.readValue(response.body(), GithubContent.class);
        } catch (Exception e) {
            System.out.println("Error in CreateFileListFromRepo");
            System.out.println(e.getMessage());
            return null;
        }
        if (content != null) {
            String cleanBase64 = content.content.replace("\n", "");
            cleanBase64 = cleanBase64.replace("\r", "");

            return new String(Base64.getDecoder().decode(cleanBase64));

        }
        return null;
    }

    //Returns a list of specified number of repos from github.com
    public static List<SimpleRepo> getRandomReposFromGithub(int numRepos) {
        List<SimpleRepo> repos;
        int page = rng.nextInt(1000/numRepos) + 1;

        String url = "https://api.github.com/search/repositories?q=stars:%3E50"
                + "&sort=stars&order=desc&per_page=" + numRepos + "&page=" + page;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token).build();

        System.out.println("Sending request to " + request.uri());


        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            SearchResponse searchResponse = mapper.readValue(response.body(),SearchResponse.class);
            System.out.println(searchResponse.items);

            /*
            for (SimpleRepo s : searchResponse.items) {
                System.out.println();
                System.out.println(s.name);
                System.out.println(s.description);
                System.out.println(s.stargazers_count);
                System.out.println(s.url);
            }
             */
            return searchResponse.items;

        } catch (Exception e) {
            System.out.println("Error in getRandomReposFromGithub");
            System.out.println(e.getMessage());
        }
        return null;
    }

    //Generate Repo List Randomly from input
    public static String generateUserOutputString(List<SimpleRepo> repos){
        StringBuilder returnStr = new StringBuilder();
        int index = 1;
        for(SimpleRepo r : repos) {
           String s = "====================\n";
           s += "Repository " + index + "\n";
           s += "Name = " + r.name + "\n";
           s += "Description = " + r.description + "\n";
           s += "====================\n";
           s += "\n";
           returnStr.append(s);
        }
        return returnStr.toString();
    }

}
