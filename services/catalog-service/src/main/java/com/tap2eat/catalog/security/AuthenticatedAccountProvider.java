package com.tap2eat.catalog.security;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedAccountProvider {

    private static final String ACCOUNT_ID_CLAIM = "accountId";

    public String getRequiredAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }

        Object accountId = jwtAuthenticationToken.getToken().getClaim(ACCOUNT_ID_CLAIM);

        if (accountId == null || accountId.toString().isBlank()) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }

        return accountId.toString();
    }
}