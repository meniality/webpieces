package org.webpieces.router.impl.ctx;

import java.util.UUID;

import org.webpieces.ctx.api.Session;
import org.webpieces.router.impl.params.ObjectTranslator;

public class SessionImpl extends CookieScopeImpl implements Session, SecureCookie {

	public static final String SECURE_TOKEN_KEY = "__ST";
	public static final String COOKIE_NAME = CookieScopeImpl.COOKIE_NAME_PREFIX+"Session";
	
	public SessionImpl(ObjectTranslator primitiveTranslator) {
		super(primitiveTranslator);
	}

	protected boolean isKeep() {
		return cookie.size() > 0;
	}
	
	@Override
	public String getName() {
		return COOKIE_NAME;
	}

	@Override
	public String fetchSecureToken()  {
        if (!containsKey(SECURE_TOKEN_KEY)) {
        	String secureToken = UUID.randomUUID().toString().replaceAll("-", "");
            put(SECURE_TOKEN_KEY, secureToken);
        }
        return get(SECURE_TOKEN_KEY);
    }
	
}
