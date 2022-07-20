package com.skillshare.demo.pojos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Comment {
    private final String author;
    private final String message;
}
