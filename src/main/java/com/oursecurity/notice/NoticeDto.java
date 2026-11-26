package com.oursecurity.notice;

import java.time.Instant;

public record NoticeDto(Long id, String title, String body, Instant createdAt, String postedByName) {

    public static NoticeDto from(Notice n) {
        return new NoticeDto(n.getId(), n.getTitle(), n.getBody(), n.getCreatedAt(), n.getPostedBy().getName());
    }
}
