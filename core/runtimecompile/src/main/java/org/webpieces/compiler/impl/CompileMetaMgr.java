package org.webpieces.compiler.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.util.file.VirtualFile;

/**
 * Application classes container.
 */
public class CompileMetaMgr {

	private static final Logger log = LoggerFactory.getLogger(CompileMetaMgr.class);
    /**
     * Cache of all compiled classes
     */
    Map<String, CompileClassMeta> classes = new HashMap<String, CompileClassMeta>();

    public CompileMetaMgr() {
	}

	/**
     * Clear the classes cache
     */
    public void clear() {
        classes = new HashMap<String, CompileClassMeta>();
    }

    public CompileClassMeta getOrCreateApplicationClass(String name, VirtualFile current) {
    	CompileClassMeta applicationClass = classes.get(name);
    	if(applicationClass != null)
    		return applicationClass;
    	else if(current == null)
    		return null;

    	if(log.isTraceEnabled())
    		log.trace("Adding class="+name+" to ApplicationClassMgr");
		CompileClassMeta appClass = new CompileClassMeta(name, current);
		classes.put(name, appClass);
		return appClass;
    }
    
    /**
     * Get a class by name
     * @param name The fully qualified class name
     * @return The ApplicationClass or null
     */
    public CompileClassMeta getApplicationClass(String name) {
    	CompileClassMeta applicationClass = classes.get(name);
    	if(applicationClass != null)
    		return applicationClass;
    	
    	//the compiler looks up packages that we don't have (or classes we don't have like java.lang.Object)
    	return null;
    }

    /**
     * All loaded classes.
     * @return All loaded classes
     */
    public List<CompileClassMeta> all() {
        return new ArrayList<CompileClassMeta>(classes.values());
    }

    /**
     * Put a new class to the cache.
     */
    public void add(CompileClassMeta applicationClass) {
        classes.put(applicationClass.name, applicationClass);
    }

    /**
     * Remove a class from cache
     */
    public void remove(CompileClassMeta applicationClass) {
        classes.remove(applicationClass.name);
    }

    public void remove(String applicationClass) {
        classes.remove(applicationClass);
    }

    /**
     * Does this class is already loaded ?
     * @param name The fully qualified class name
     */
    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }
    
    @Override
    public String toString() {
        return classes.toString();
    }
}