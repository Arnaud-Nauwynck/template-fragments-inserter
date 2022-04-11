package fr.an.templatefragmentinserter.model;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableList;

public abstract class TemplateDef {

	public final String name;
	
	public final ImmutableList<TemplateParamDef> paramDefs;
	
	public TemplateDef(String name, ImmutableList<TemplateParamDef> paramDefs) {
		this.name = name;
		this.paramDefs = paramDefs;
	}



	public abstract InsertTemplateResult insertTemplate( 
			File baseDir, Map<Object,Object> params);
	
}
