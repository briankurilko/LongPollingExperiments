package com.skillshare.demo.pojos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class Talk {
    private final String title;
    private final String presenter;
    private final String summary;
    private final List<Comment> comments;
}
