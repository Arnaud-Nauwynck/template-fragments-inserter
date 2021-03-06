package fr.an.templatefragmentinserter.model;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Map;

import lombok.val;

public abstract class TemplateDef {

	public final String name;
	
	public final ImmutableList<TemplateParamDef> paramDefs;
	
	public TemplateDef(String name, ImmutableList<TemplateParamDef> paramDefs) {
		this.name = name;
		this.paramDefs = paramDefs;
	}



	public abstract InsertTemplateResult insertTemplate( 
			File baseDir, Map<Object,Object> params);
	
	
	public final TemplateParamDef findParamDefByName(String name) {
	    for(val e : paramDefs) {
	        if (e.name.equals(name)) {
	            return e;
	        }
	    }
	    return null;
	}

}
