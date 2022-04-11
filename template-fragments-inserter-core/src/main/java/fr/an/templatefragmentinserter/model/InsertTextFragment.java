package fr.an.templatefragmentinserter.model;

import java.io.File;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InsertTextFragment {

	public final File toFile;
	public final int lineNumber;
	public final String insertText;
	
	@Override
	public String toString() {
		return "Insert[to=" + toFile + ", line#" + lineNumber + ", Text=" + insertText
				+ "]";
	}
	
}
