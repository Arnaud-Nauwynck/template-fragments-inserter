package fr.an.templatefragmentinserter.conf;

public class SimpleTemplateDefConf {
	
    public String name;
    
	public String to;
	
	public String template;
    public String templateFile;

	// public String templateType = "freemaker"; // "velocity", ..

	public boolean inject = true;
	public String after;
	public String before;

	public boolean force = false;
	public boolean unlessExists = false;
	
	// skip_if:	Regex	undefined	myPackage
}