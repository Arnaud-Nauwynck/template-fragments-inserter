package fr.an.templatefragmentinserter.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ImmutableList;

import fr.an.templatefragmentinserter.conf.SimpleTemplateDefConf;
import fr.an.templatefragmentinserter.conf.TemplateDefsConf;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.val;

public class TemplateDefParser {

	private static final String LINE_SEP = "---";

	private static final Yaml yaml = new Yaml();
	
	public static void parseYamlTemplates(Map<String, TemplateDef> resDefs, File codeTemplateFile) {
		TemplateDefsConf templateDefsConf;
		try (val reader = new InputStreamReader(new FileInputStream(codeTemplateFile))) {
			templateDefsConf = yaml.loadAs(reader, TemplateDefsConf.class);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException("File not found " + codeTemplateFile, ex);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read file " + codeTemplateFile, ex);
		}
		Configuration freeMakerConfig = new Configuration(); // Configuration.getIncompatibleImprovements());
		for(val def: templateDefsConf.defs) {
			val paramDefs = ImmutableList.<TemplateParamDef>builder();
			if (def.params != null) {
				for(val paramConf: def.params) {
					val paramDef = new TemplateParamDef(paramConf.name, paramConf.defaultValue);
					paramDefs.add(paramDef);
				}
			}
			
			Template toTemplate = textToTemplate("to", def.to, freeMakerConfig);
			Template template = textToTemplate("template", def.template, freeMakerConfig);
			Template after = (def.after != null)? textToTemplate("after", def.after, freeMakerConfig) : null;
			Template before = (def.before != null)? textToTemplate("before", def.before, freeMakerConfig) : null;
			   
			TemplateDef templateDef = new SimpleTemplateInsertFragmentDef(
					def.name, paramDefs.build(), //
					toTemplate, template, //
					def.inject, // 
					after, before, //
					def.force,
					def.unlessExists
					);
			resDefs.put(def.name, templateDef);
		}
	}

	private static Template textToTemplate(String name, String text, Configuration freeMakerConfig) {
		try {
			return new Template(name, text, freeMakerConfig);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void parseTemplates(Map<String, TemplateDef> defs, File codeTemplateFile) {
		try (val lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(codeTemplateFile)))) {
			readUntilLineSep(lineReader);
			SimpleTemplateDefConf curr = new SimpleTemplateDefConf();
			for(;;) {
				val line = lineReader.readLine();
				if (line == null) {
					break;
				}
				int indexSep = line.indexOf(":");
				if (indexSep != -1) {
					String key = line.substring(0, indexSep).trim().toLowerCase();
					String value = line.substring(indexSep+1, line.length()).trim();
						
					if (key.equalsIgnoreCase("name")) {
						curr.name = value;
					} else if (key.equalsIgnoreCase("to")
							|| key.equalsIgnoreCase("toFile") 
							|| key.equalsIgnoreCase("to_file") 
							|| key.equalsIgnoreCase("toFilePattern") 
							) {
						curr.to = value;
					} else if (key.equalsIgnoreCase("from")
							// || key.equalsIgnoreCase("fromTemplate") 
							) {
						curr.template = value;
//					} else if (key.equalsIgnoreCase("templateType")
//							|| key.equalsIgnoreCase("template_type")
//							) {
//						curr.templateType = value;
					} else if (key.equalsIgnoreCase("inject")) {
						curr.inject = Boolean.valueOf(value);
					} else if (key.equalsIgnoreCase("force")) {
						curr.force = Boolean.valueOf(value);
					} else if (key.equalsIgnoreCase("unlessExist")
							|| key.equalsIgnoreCase("unless_exist")
							) {
						curr.unlessExists = Boolean.valueOf(value);
					} else if (key.equalsIgnoreCase("after")) {
						curr.after = value;
					} else if (key.equalsIgnoreCase("before")) {
						curr.before = value;
					} else {
						throw new IllegalArgumentException("unrecognized property '" + key + "' in " + codeTemplateFile);
					}
				} else if (line.startsWith(LINE_SEP)) {
					curr.template = readUntilLineSep(lineReader);
					
					// defs.put(curr.name, );
					curr = new SimpleTemplateDefConf();
				} else {
					throw new IllegalArgumentException("unrecognized line '" + line + "' in " + codeTemplateFile);
				}
			}
		} catch (FileNotFoundException ex) {
			throw new RuntimeException("File not found " + codeTemplateFile, ex);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read file " + codeTemplateFile, ex);
		}
	}

	private static String readUntilLineSep(BufferedReader lineReader) throws IOException {
		val sb = new StringBuilder(); 
		for(;;) {
			val line = lineReader.readLine();
			if (line == null) {
				break; // EOF
			}
			if (LINE_SEP.equals(line)) {
				break;
			}
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}

}
