package searchengine.searchComponents;



public record SearchDTO(String site, String siteName, String uri, String title, String snippet, float relevance) {
}
