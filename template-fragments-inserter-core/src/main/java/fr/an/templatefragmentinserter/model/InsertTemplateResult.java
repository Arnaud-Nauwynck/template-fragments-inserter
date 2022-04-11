package fr.an.templatefragmentinserter.model;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InsertTemplateResult {

	public final List<InsertTextFragment> inserts;

	@Override
	public String toString() {
		return "inserts:" + inserts;
	}

}
