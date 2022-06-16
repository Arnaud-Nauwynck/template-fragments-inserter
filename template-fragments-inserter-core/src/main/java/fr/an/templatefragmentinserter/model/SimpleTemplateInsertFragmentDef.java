package fr.an.templatefragmentinserter.model;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.ImmutableList;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleTemplateInsertFragmentDef extends TemplateDef {

	public final Template toTemplate;
	
	public final Template template;

	public final boolean inject;
	public final Template after;
	public final Template before;

	public final boolean force;
	public final boolean unlessExists;
	
	// ------------------------------------------------------------------------
	
	public SimpleTemplateInsertFragmentDef(String name, 
			ImmutableList<TemplateParamDef> paramDefs,
			Template toTemplate, 
			Template template, 
			boolean inject, 
			Template after, 
			Template before, 
			boolean force, boolean unlessExists) {
		super(name, paramDefs);
		this.toTemplate = toTemplate;
		this.template = template;
		this.inject = inject;
		this.after = after;
		this.before = before;
		this.force = force;
		this.unlessExists = unlessExists;
	}

	// ------------------------------------------------------------------------
	
	@Override
	public InsertTemplateResult insertTemplate( 
			File baseDir,
			Map<Object,Object> params) {
		val inserts = new ArrayList<InsertTextFragment>();
		
		String toPath = evalTemplateString("to", toTemplate, params);
		log.info("toPath:" + toPath);
		
		String text = evalTemplateString("template", template, params);
		log.info("text:" + text);
		
		List<String> textLines = Arrays.asList(text.split("\n"));
		
		
		File toFile = new File(baseDir, toPath);
		File toParentDir = toFile.getParentFile();
		if (! toParentDir.exists()) {
			toParentDir.mkdirs();
		}

		int lineNumber = -1;
		List<String> fileLines;
		if (toFile.exists()) {
			if (unlessExists) {
				return new InsertTemplateResult(inserts);
			}
			fileLines = readFileLines(toFile);
			
			int fileLineCount = fileLines.size();
			if (after != null) {
				String afterText = evalTemplateString("after", after, params);
				log.info("after:" + afterText);
				Pattern afterPattern = Pattern.compile(afterText);
				for(int i = 0; i < fileLineCount; i++) {
					val line = fileLines.get(i);
					if (afterPattern.matcher(line).matches()) {
						lineNumber = i + 1;

						log.info("insert text " + textLines.size() + " lines after line: " + lineNumber + ":\n" + String.join("\n", textLines));
						fileLines.addAll(lineNumber, textLines);
						inserts.add(new InsertTextFragment(toFile, lineNumber, text));
						
						break;
					}
				}
				if (lineNumber == -1) {
				    log.error("after text NOT FOUND: " + afterText);
				}
				
			} else if (before != null) {
				String beforeText = evalTemplateString("before", before, params);
				log.info("before:" + beforeText);
				Pattern beforePattern = Pattern.compile(beforeText);
				for(int i = 0; i < fileLineCount; i++) {
					val line = fileLines.get(i);
					if (beforePattern.matcher(line).matches()) {
						lineNumber = i;

						log.info("insert text " + textLines.size() + " lines before line: " + lineNumber + ":\n" + String.join("\n", textLines));

						fileLines.addAll(lineNumber, textLines);
						inserts.add(new InsertTextFragment(toFile, lineNumber, text));

						break;
					}
				}

				if (lineNumber == -1) {
                    log.error("before text NOT FOUND: " + beforeText);
                }
			} else if (force) {
			    log.info("force overwrite text " + textLines.size() + " lines:\n" + String.join("\n", textLines));

			    lineNumber = 0;

			    fileLines.clear();
                fileLines.addAll(textLines);
                inserts.add(new InsertTextFragment(toFile, 0, text));
			} else {
			    // ??? 
                log.info("??? unknown before/after/force ... append text " + textLines.size() + " lines:\n" + String.join("\n", textLines));

                lineNumber = fileLines.size();
                fileLines.addAll(textLines);
                inserts.add(new InsertTextFragment(toFile, 0, text));
			    
			}
		} else {
			// error or full content?
			lineNumber = 0;
			fileLines = new ArrayList<>();

			log.info("insert text " + textLines.size() + " lines\n" + String.join("\n", textLines));

			fileLines.addAll(lineNumber, textLines);
			inserts.add(new InsertTextFragment(toFile, lineNumber, text));
		}
		
		if (inserts.isEmpty()) {
			// pattern not found? .. nothing to save
		} else {
			// save file
			writeFileLines(toFile, fileLines);
		}
		
		return new InsertTemplateResult(inserts);
	}

	private List<String> readFileLines(File toFile) {
		try {
			return FileUtils.readLines(toFile, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read file " + toFile, ex);
		}
	}
	
	private void writeFileLines(File toFile, List<String> lines) {
		try {
			FileUtils.writeLines(toFile, lines);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to write file " + toFile, ex);
		}
	}
	
	protected String evalTemplateString(String displayMsg,
	        Template template, Map<Object,Object> params) {
		StringWriter res = new StringWriter();
		try {
			template.process(params, res);
		} catch (TemplateException ex) {
			throw new RuntimeException("Failed to eval template " + displayMsg, ex);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to eval template " + displayMsg, ex);
		} catch(RuntimeException ex) {
		    throw new RuntimeException("Failed to eval template " + displayMsg, ex);
		}
		return res.toString();
	}
}
