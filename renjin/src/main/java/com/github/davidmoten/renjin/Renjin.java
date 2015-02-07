package com.github.davidmoten.renjin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Renjin {

	public static void execute(InputStream is) {
		// create a script engine manager:
		ScriptEngineManager manager = new ScriptEngineManager();
		// create a Renjin engine:
		ScriptEngine engine = manager.getEngineByName("Renjin");
		// check if the engine has loaded correctly:
		if (engine == null) {
			throw new RuntimeException(
					"Renjin Script Engine not found on the classpath.");
		}
		try {
			engine.eval(new InputStreamReader(is, Charset.forName("UTF-8")));
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}

}
