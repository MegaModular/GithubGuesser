import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleRepo {
    public String name;
    public String description;
    public String url;
    public String full_name;
    public int stargazers_count;
}
