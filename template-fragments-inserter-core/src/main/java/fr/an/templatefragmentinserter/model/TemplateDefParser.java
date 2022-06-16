package fr.an.templatefragmentinserter.model;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import fr.an.templatefragmentinserter.conf.CompositeTemplateDefConf;
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
		File parentDir = codeTemplateFile.getParentFile();
		Configuration freeMakerConfig = new Configuration(); // Configuration.getIncompatibleImprovements());
		for(CompositeTemplateDefConf compositeTemplateConf: templateDefsConf.defs) {
			val templateDef = toCompositeTemplateDef(parentDir, freeMakerConfig, compositeTemplateConf);
			
			resDefs.put(compositeTemplateConf.name, templateDef);
		}
	}

    private static CompositeTemplateInsertFragmentDef toCompositeTemplateDef(
            File parentDir, 
            Configuration freeMakerConfig, 
            CompositeTemplateDefConf def) {
        val paramDefsBuilder = ImmutableList.<TemplateParamDef>builder();
        if (def.params != null) {
        	for(val paramConf: def.params) {
        		val paramDef = new TemplateParamDef(paramConf.name, paramConf.defaultValue);
        		paramDefsBuilder.add(paramDef);
        	}
        }
        val paramDefs = paramDefsBuilder.build();
        val elementsBuilder = ImmutableList.<TemplateDef>builder();
        for(val simpleTemplateDef: def.elements) {
            val element = confToSimpleTemplateDef(parentDir, freeMakerConfig, def, paramDefs, simpleTemplateDef);
            elementsBuilder.add(element);
        }
        val elements = elementsBuilder.build();
        val templateDef = new CompositeTemplateInsertFragmentDef(def.name, paramDefs, elements);
        return templateDef;
    }

    private static SimpleTemplateInsertFragmentDef confToSimpleTemplateDef(File parentDir, Configuration freeMakerConfig,
            CompositeTemplateDefConf def,
            ImmutableList<TemplateParamDef> paramDefs,
            SimpleTemplateDefConf src) {
        Template toTemplate = textToTemplate("to", src.to, freeMakerConfig);
        
        String templateText = src.template;
        if (src.template == null || src.template.isEmpty()) {
            File templateFile = new File(parentDir, src.templateFile);
            try {
                templateText = FileUtils.readFileToString(templateFile, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read file " + templateFile, ex);
            }
        }
        Template template = textToTemplate("template", templateText, freeMakerConfig);
        
        Template after = (src.after != null)? textToTemplate("after", src.after, freeMakerConfig) : null;
        Template before = (src.before != null)? textToTemplate("before", src.before, freeMakerConfig) : null;
           
        val res = new SimpleTemplateInsertFragmentDef(
                def.name, paramDefs, //
                toTemplate, template, //
                src.inject, // 
                after, before, //
                src.force,
                src.unlessExists
                );
        return res;
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
