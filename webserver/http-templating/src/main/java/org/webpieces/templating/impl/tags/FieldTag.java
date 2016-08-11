package org.webpieces.templating.impl.tags;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.webpieces.templating.api.ClosureUtil;
import org.webpieces.templating.api.HtmlTag;
import org.webpieces.templating.impl.GroovyTemplateSuperclass;

import groovy.lang.Closure;

public class FieldTag extends TemplateLoaderTag implements HtmlTag {

	private String fieldHtmlPath;

	public FieldTag(String fieldHtmlPath) {
		this.fieldHtmlPath = fieldHtmlPath;
	}

	@Override
	public String getName() {
		return "field";
	}

	@Override
	protected String getFilePath(GroovyTemplateSuperclass callingTemplate, Map<Object, Object> args, String srcLocation) {
		return fieldHtmlPath;
	}
	
	@Override
	protected Map<String, Object> convertTagArgs(Map<Object, Object> tagArgs, Map<String, Object> pageArgs, Closure<?> body, String srcLocation) {
		if(tagArgs.get("_body") != null)
			throw new IllegalArgumentException("tag "+getName()+" must not define an argument of '_body' as that is reserved and will be overwritten");
		else if(tagArgs.get("field") != null)
			throw new IllegalArgumentException("tag "+getName()+" must not define an argument of 'field' as that is reserved and will be overwritten ");

        String _arg = tagArgs.get("_arg").toString();
		Map<String, Object> field = createFieldData(_arg, pageArgs);
        
		Map<String, Object> copy = new HashMap<>();
		for(Map.Entry<Object, Object> entry : tagArgs.entrySet()) {
			String key = entry.getKey().toString();
			copy.put(key, entry.getValue());
			body.setProperty(key, entry.getValue());
		}
		
		copy.put("field", field);
		
		String bodyStr = "";
		if(body != null) {
			body.setProperty("field", field);
			bodyStr = ClosureUtil.toString(body);
		}
		//variables starting with _ will not be html escaped so the body html won't be converted like other variables
		copy.put("_body", bodyStr); 
		
		return copy;
	}

	private Map<String, Object> createFieldData(String _arg, Map<String, Object> pageArgs) {
        Map<String, Object> field = new HashMap<String, Object>();
        field.put("name", _arg);
        field.put("id", _arg.replace('.', '_'));
        //field.put("flash", Flash.current().get(_arg));
        //field.put("flashArray", field.get("flash") != null && !StringUtils.isEmpty(field.get("flash").toString()) ? field.get("flash")
        //        .toString().split(",") : new String[0]);
        //field.put("error", Validation.error(_arg));
        //field.put("errorClass", field.get("error") != null ? "hasError" : "");
        String[] pieces = _arg.split("\\.");
        Object obj = pageArgs.get(pieces[0]);
        if (pieces.length > 1) {
            try {
                String path = _arg.substring(_arg.indexOf(".") + 1);
                Object value = PropertyUtils.getProperty(obj, path);
                field.put("value", value);
            } catch (Exception e) {
                // if there is a problem reading the field we dont set any
                // value
            }
        } else {
            field.put("value", obj);
        }
        
		return field;
	}
}