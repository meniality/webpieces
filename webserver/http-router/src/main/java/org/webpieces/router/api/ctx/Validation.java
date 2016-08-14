package org.webpieces.router.api.ctx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webpieces.router.api.dto.RouterCookie;

public class Validation extends FlashScope {
	
	public Validation(CookieFactory creator) {
		super(creator);
	}

	private Map<String, List<String>> fieldErrors = new HashMap<>();

	public void addError(String name, String error) {
		List<String> list = fieldErrors.get(name);
		if(list == null) {
			list = new ArrayList<>();
			fieldErrors.put(name, list);
		}
		list.add(error);
	}	
	
	public boolean hasErrors() {
		if(fieldErrors.size() > 0)
			return true;
		return false;
	}

	@Override
	protected RouterCookie toCookie(Integer maxAge) {
		return creator.createCookie(CookieFactory.COOKIE_NAME_PREFIX+"Errors", fieldErrors, maxAge);
	}

}
