package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;

public interface OAuthProviderClient {

    AuthProvider provider();

    OAuthUserProfile verify(OAuthLoginRequest request);
}
