package searchengine.searchComponents;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import searchengine.searchComponents.SearchDTO;
import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {

    private boolean result;
    private String error;
    private int count;
    private List<SearchDTO> data;
    private HttpStatus status;

    public SearchResponse(boolean result, String error, HttpStatus status) {
        this.result = result;
        this.error = error;
        this.status = status;
    }
}

