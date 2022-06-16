package fr.an.templatefragmentinserter.model;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeTemplateInsertFragmentDef extends TemplateDef {

	public final List<TemplateDef> elements;
	
	// ------------------------------------------------------------------------
	
	public CompositeTemplateInsertFragmentDef(String name, 
			ImmutableList<TemplateParamDef> paramDefs,
			ImmutableList<TemplateDef> elements) {
		super(name, paramDefs);
		this.elements = elements;
	}

	// ------------------------------------------------------------------------
	
	@Override
	public InsertTemplateResult insertTemplate( 
			File baseDir,
			Map<Object,Object> params) {
		val inserts = new ArrayList<InsertTextFragment>();

		for(val element: elements) {
		    try {
		        val tmpres = element.insertTemplate(baseDir, params);
		        inserts.addAll(tmpres.inserts);
		    } catch(Exception ex) {
		        String errorMsg = "Failed to generate template element '" + element.name + "' ";
		        log.error(errorMsg + ", ex: " + ex.getMessage());
		        throw new RuntimeException(errorMsg, ex);
		    }
		}
		
		return new InsertTemplateResult(inserts);
	}
}
