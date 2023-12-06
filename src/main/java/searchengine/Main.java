//package searchengine;
//
//import searchengine.dto.PageDTO;
//import searchengine.services.impl.ParseHTMLImpl;
//
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.ForkJoinPool;
//
//public class Main {
//     public static String url = "https://www.lutherancathedral.ru/";
//    public static void main(String[] args) {
//        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
//        Set<String> urlsSet = ConcurrentHashMap.newKeySet();
//        List<PageDTO> pageDtoList = new CopyOnWriteArrayList<>();
//        List<PageDTO> pages = forkJoinPool
//                .invoke(new ParseHTMLImpl(url, urlsSet, pageDtoList));
//
//        pages.forEach(page -> System.out.println(page));
//    }
//}
