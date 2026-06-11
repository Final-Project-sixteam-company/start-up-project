package com.startup.domain.community.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements ErrorCode {

    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "COMMUNITY_001", "이미 북마크한 시나리오입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_002", "북마크를 찾을 수 없습니다."),
    
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "COMMUNITY_003", "이미 리뷰를 작성한 시나리오입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_004", "리뷰를 찾을 수 없습니다."),
    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "COMMUNITY_005", "리뷰 작성자만 수정/삭제할 수 있습니다."),
    CANNOT_REVIEW_OWN(HttpStatus.FORBIDDEN, "COMMUNITY_006", "자신의 시나리오에는 리뷰를 작성할 수 없습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "COMMUNITY_007", "별점은 1~5점 사이여야 합니다."),
    
    ALREADY_REPORTED(HttpStatus.CONFLICT, "COMMUNITY_008", "이미 신고한 시나리오입니다."),
    
    SCENARIO_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "COMMUNITY_009", "배포된 시나리오에만 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
