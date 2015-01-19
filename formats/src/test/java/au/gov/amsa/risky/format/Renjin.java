package au.gov.amsa.risky.format;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Renjin {

	static void call(String resourceName){
		// create a script engine manager:
	    ScriptEngineManager manager = new ScriptEngineManager();
	    // create a Renjin engine:
	    ScriptEngine engine = manager.getEngineByName("Renjin");
	    // check if the engine has loaded correctly:
	    if(engine == null) {
	        throw new RuntimeException("Renjin Script Engine not found on the classpath.");
	    }
	    InputStreamReader reader = new InputStreamReader(RenjinTest.class.getResourceAsStream(resourceName));
	    try {
	    	engine.eval(reader);
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}
	
}
