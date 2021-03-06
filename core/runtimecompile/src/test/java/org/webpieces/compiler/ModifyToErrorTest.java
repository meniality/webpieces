package org.webpieces.compiler;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.webpieces.compiler.api.CompilationsException;
import org.webpieces.compiler.api.CompileError;


public class ModifyToErrorTest extends AbstractCompileTest {

	@Override
	protected String getPackageFilter() {
		return "org.webpieces.compiler.error";
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleChangeMethodNameAndRetVal() {
		LOG.info("loading class ErrorController");
		//DO NOT CALL Classname.getClass().getName() so that we don't pre-load it from the default classloader and
		//instead just tediously form the String ourselves...
		Class c = compiler.loadClass("org.webpieces.compiler.error.ErrorController");

		LOG.info("loaded");
		int retVal = invokeMethodReturnInt(c, "someMethod");
		
		Assert.assertEquals(5, retVal);
		
		cacheAndMoveFiles();
		
		try {
			compiler.loadClass("org.webpieces.compiler.error.ErrorController");
		} catch(CompilationsException exc) {
			CompileError e = exc.getCompileErrors().get(0);
			
			List<String> source = e.getBadSourceLine();
			Assert.assertTrue(e.getMessage().contains("The method noMethodExists() is undefined"));
			Assert.assertEquals(9, e.getProblem().getSourceLineNumber());
			Assert.assertTrue(e.getJavaFile().getAbsolutePath().endsWith("ErrorController.java"));
			//verify the source line 9 is at the 8th position in list of Strings
			Assert.assertTrue(source.get(1).contains("new ChildClassNoError(). noMethodExists()"));
		}


	}


}
