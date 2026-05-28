package com.dwsc.backend.news;

import com.dwsc.backend.news.dto.NewsResponse;
import com.dwsc.corba.news.client.CorbaNewsClient;
import com.dwsc.corba.news.client.NewsRow;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CorbaNewsService {

    @Value("${corba.news.host:127.0.0.1}")
    private String orbHost;

    @Value("${corba.news.port:1050}")
    private int orbPort;

    @Value("${corba.news.serviceName:NewsService}")
    private String serviceName;

    public List<NewsResponse> listNews() {
        try {
            CorbaNewsClient client = new CorbaNewsClient(orbHost, orbPort, serviceName);
            return client.listNews().stream().map(CorbaNewsService::map).toList();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CORBA News service is unavailable. Start Naming Service and News Producer.",
                    ex);
        }
    }

    public NewsResponse getNewsById(String id) {
        return listNews().stream()
                .filter(n -> n.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News item not found"));
    }

    private static NewsResponse map(NewsRow row) {
        return new NewsResponse(row.id(), row.title(), row.content(), row.date());
    }
}
