package fr.an.templatefragmentinserter.conf;

public class SimpleTemplateDefConf extends TemplateDefConf {
	
	public String to;
	
	public String template;

	// public String templateType = "freemaker"; // "velocity", ..

	public boolean inject = true;
	public String after;
	public String before;

	public boolean force = false;
	public boolean unlessExists = false;
	
	// skip_if:	Regex	undefined	myPackage
}