import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubContent {
    public String name;
    public String path;
    public String type;

    @JsonProperty("_links")
    private void unpackLinks(Map<String, String> links) {
        this.self = links.get("self");
    }

    public String self;

    public String toString() {
        String s = "\nName = ";
        s += name;
        s += "\nPath = ";
        s += path;
        s += "\nType = ";
        s += type;
        return s;
    }
}

