package com.url.shortener.service;

import com.url.shortener.dtos.ClickEventDTO;
import com.url.shortener.dtos.UrlMappingDTO;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {

    private UrlMappingRepository urlMappingRepository;
    private ClickEventRepository clickEventRepository;

    // CREATE SHORT URL
    public UrlMappingDTO createShortUrl(String originalUrl, User user) {

        String shortUrl = generateShortUrl();

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        urlMapping.setClickCount(0);

        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);

        return convertToDto(savedUrlMapping);
    }

    // CONVERT ENTITY → DTO
    private UrlMappingDTO convertToDto(UrlMapping urlMapping){

        UrlMappingDTO dto = new UrlMappingDTO();

        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setClickCount(urlMapping.getClickCount());
        dto.setCreatedDate(urlMapping.getCreatedDate());
        dto.setUsername(urlMapping.getUser().getUsername());

        return dto;
    }

    // GENERATE UNIQUE SHORT URL
    private String generateShortUrl() {

        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();

        String shortUrl;

        do {

            StringBuilder builder = new StringBuilder(8);

            for (int i = 0; i < 8; i++) {

                builder.append(
                        characters.charAt(
                                random.nextInt(characters.length())
                        )
                );

            }

            shortUrl = builder.toString();

        } while (urlMappingRepository.findByShortUrl(shortUrl) != null);

        return shortUrl;
    }

    // GET USER SHORT URLS
    public List<UrlMappingDTO> getUrlsByUser(User user) {

        return urlMappingRepository
                .findByUser(user)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    // CLICK EVENTS BY DATE
    public List<ClickEventDTO> getClickEventsByDate(String shortUrl,
                                                    LocalDateTime start,
                                                    LocalDateTime end) {

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            return List.of(); // never return null
        }

        return clickEventRepository
                .findByUrlMappingAndClickDateBetween(urlMapping, start, end)
                .stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickDate().toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> {

                    ClickEventDTO dto = new ClickEventDTO();
                    dto.setClickDate(entry.getKey());
                    dto.setCount(entry.getValue());

                    return dto;

                })
                .toList();
    }

    // TOTAL CLICKS ANALYTICS
    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user,
                                                            LocalDate start,
                                                            LocalDate end) {

        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);

        // ⭐ FIX FOR EMPTY URL LIST
        if (urlMappings == null || urlMappings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ClickEvent> clickEvents =
                clickEventRepository
                        .findByUrlMappingInAndClickDateBetween(
                                urlMappings,
                                start.atStartOfDay(),
                                end.plusDays(1).atStartOfDay()
                        );

        if (clickEvents == null || clickEvents.isEmpty()) {
            return Collections.emptyMap();
        }

        return clickEvents
                .stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickDate().toLocalDate(),
                        Collectors.counting()
                ));
    }

    // REDIRECT SHORT URL
    public UrlMapping getOriginalUrl(String shortUrl) {

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping != null) {

            urlMapping.setClickCount(urlMapping.getClickCount() + 1);

            urlMappingRepository.save(urlMapping);

            // SAVE CLICK EVENT

            ClickEvent clickEvent = new ClickEvent();

            clickEvent.setClickDate(LocalDateTime.now());
            clickEvent.setUrlMapping(urlMapping);

            clickEventRepository.save(clickEvent);1
        }

        return urlMapping;
    }
}