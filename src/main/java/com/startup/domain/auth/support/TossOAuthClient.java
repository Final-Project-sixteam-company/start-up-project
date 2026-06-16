package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.TossLoginRequest;

public interface TossOAuthClient {

    OAuthUserProfile verify(TossLoginRequest request);
}
