package com.dwsc.comment.comment;

import com.dwsc.comment.api.dto.AuthorCommentItem;
import com.dwsc.comment.api.dto.PlayerCommentResponse;
import com.dwsc.comment.model.entity.Comment;

public final class CommentMapper {
    private CommentMapper() {}

    public static PlayerCommentResponse toPlayerCommentResponse(Comment c) {
        return new PlayerCommentResponse(
                c.getId().toString(),
                c.getAuthor(),
                c.getAuthorName(),
                c.getText(),
                c.getRating() != null ? c.getRating() : 0,
                c.getLocation(),
                c.getCreatedAt());
    }

    public static AuthorCommentItem toAuthorCommentItem(Comment c) {
        return new AuthorCommentItem(
                c.getId().toString(),
                c.getPlayerId().toString(),
                c.getText(),
                c.getRating() != null ? c.getRating() : 0,
                c.getAuthor(),
                c.getAuthorName(),
                c.getCreatedAt());
    }
}

