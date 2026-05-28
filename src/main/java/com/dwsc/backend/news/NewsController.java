package com.dwsc.backend.news;

import com.dwsc.backend.news.dto.NewsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "CORBA-backed player news feed")
public class NewsController {

    private final CorbaNewsService corbaNewsService;

    public NewsController(CorbaNewsService corbaNewsService) {
        this.corbaNewsService = corbaNewsService;
    }

    @Operation(summary = "List player news")
    @GetMapping
    public ResponseEntity<List<NewsResponse>> listNews() {
        return ResponseEntity.ok(corbaNewsService.listNews());
    }

    @Operation(summary = "Get one player news item by id")
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNews(@PathVariable("id") String id) {
        return ResponseEntity.ok(corbaNewsService.getNewsById(id));
    }
}
