package searchengine.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DtoStartIndexing {

    private boolean result;

    private String error;

}

