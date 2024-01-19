package searchengine.searchComponents;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data

public class SearchConfig {
    private String query;
    private String site;
    private int offset;
    private int limit;

}
