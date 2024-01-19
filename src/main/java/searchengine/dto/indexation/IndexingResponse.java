package searchengine.dto.indexation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String error;
    private HttpStatus status;


    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}

