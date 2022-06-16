package fr.an.templatefragmentinserter.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDefRegistry {
	
	private final String codeTemplatesDirName = "_code_templates";

	public static class TemplatesLookupContext {
		private /* Immutable */ Map<String, TemplateDef> defs;

		public TemplatesLookupContext(Map<String, TemplateDef> defs) {
			this.defs = defs;
		}
		
		public TemplateDef get(String name) {
			return defs.get(name);
		}
		public Map<String,TemplateDef> getDefs() {
			return new HashMap<>(defs);
		}
	}

	private final Object lock = new Object();

	private TemplatesLookupContext globalUserContext;

	// @GuardedBy("lock")
	private final Map<File, TemplatesLookupContext> cachedContextPerDir = new HashMap<>();

	// ------------------------------------------------------------------------

	public void init() {
		//
	}

	public void reloadForDir(final File dir) {
	    synchronized (lock) {
	        cachedContextPerDir.clear();
	    }
	    lookupContextForDir(dir);
	}
	
	public TemplatesLookupContext lookupContextForDir(final File dir) {
		TemplatesLookupContext res;
		TemplatesLookupContext cachedAncestorContext = null;
		List<File> ancestorDirs = new ArrayList<>();
		synchronized (lock) {
			res = cachedContextPerDir.get(dir);
			if (res != null) {
				return res;
			}
			File ancestorDir = dir.getAbsoluteFile();
			for (; ancestorDir != null; ancestorDir = ancestorDir.getParentFile()) {
				TemplatesLookupContext foundCtx = cachedContextPerDir.get(ancestorDir);
				if (foundCtx != null) {
					cachedAncestorContext = foundCtx;
					break;
				}
				ancestorDirs.add(ancestorDir);
			}
		}
		// lookup until cachedAncestorDir, or top level dir if null
		// resolved from ancestor down to dir, find correspdoning conf
		// do read config file, and put in cache
		TemplatesLookupContext currContext = cachedAncestorContext;
		if (currContext == null) {
			synchronized (lock) {
				if (globalUserContext == null) {
					TemplatesLookupContext emptyContext = new TemplatesLookupContext(new HashMap<>());
					File userHomeDir = new File(System.getProperty("user.home"));
					this.globalUserContext = subDirContext(emptyContext, userHomeDir);
				}
				currContext = this.globalUserContext;
			}
		}
		final int ancestorLen = ancestorDirs.size();
		for (int i = ancestorLen - 1; i >= 0; i--) {
			File ancestorDir = ancestorDirs.get(i);
			TemplatesLookupContext ancestorContext = subDirContext(currContext, ancestorDir);
			if (ancestorContext != currContext) {
				currContext = ancestorContext;
				synchronized (lock) {
					cachedContextPerDir.put(ancestorDir, ancestorContext);
				}
			} // else put in WeakHashMap ?
		}
		return currContext;
	}

	private TemplatesLookupContext subDirContext(TemplatesLookupContext parentContext, File dir) {
		File codeTemplatesDir = new File(dir, codeTemplatesDirName);
		if (codeTemplatesDir.exists() && codeTemplatesDir.isDirectory()) {
			Map<String, TemplateDef> defs = new HashMap<>(parentContext.defs);
			File[] codeTemplateFiles = codeTemplatesDir.listFiles(); // may be recursive..

			for (File codeTemplateFile : codeTemplateFiles) {
				String fileName = codeTemplateFile.getName();
				if (fileName.endsWith(".yml")) {
					TemplateDefParser.parseYamlTemplates(defs, codeTemplateFile);
				}
			}
			return new TemplatesLookupContext(defs);
		} else {
			return parentContext;
		}
	}

}
