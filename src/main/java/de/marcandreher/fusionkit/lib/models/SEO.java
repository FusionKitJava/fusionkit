package de.marcandreher.fusionkit.lib.models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SEO {
    private String title;
    private String description;
    private String keywords;
    private String url;
    private String image;
    private String author;
    private String type;
}
