package fr.an.templatefragmentinserter.springshell.command;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.component.StringInput;
import org.springframework.shell.component.StringInput.StringInputContext;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import fr.an.templatefragmentinserter.model.InsertTemplateResult;
import fr.an.templatefragmentinserter.model.TemplateDef;
import fr.an.templatefragmentinserter.model.TemplateDefRegistry;
import fr.an.templatefragmentinserter.model.TemplateDefRegistry.TemplatesLookupContext;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@ShellComponent
@ShellCommandGroup("templates")
@Slf4j
public class InsCommand extends AbstractShellComponent {
	
	@Autowired
	protected TemplateDefRegistry templateDefRegistry;
	
	protected static Map<String,String> currTemplateParams = new HashMap<>();

	protected static File currDir = new File(".");
	
	/**
	 * sample usage
	 * <PRE>
	 * set-curr-dirr --dir .
	 * </PRE> 
	 */
	@ShellMethod(value = "set current directory for Templates insertion snippets",
			key = { "set-curr-dir" }
			)
	public void setCurrTemplateParam(
			@ShellOption("dir") File dir			
			) {
		InsCommand.currDir = dir;
	}
	
	/**
	 * sample usage
	 * <PRE>
	 * set-curr-template-param --key key1 --value value1
	 * </PRE> 
	 */
	@ShellMethod(value = "set current template param key - value",
			key = { "set-curr-template-param" })
	public void setCurrTemplateParam(
			@ShellOption(value = {"key"}) String key,
			@ShellOption(value = {"value"}) String value			
			) {
		if (value == null || value.equals("null")) {
			InsCommand.currTemplateParams.remove(key);
		} else {
			InsCommand.currTemplateParams.put(key,  value);
		}
	}

	/**
	 * sample usage
	 * <PRE>
	 * show-curr-template-params
	 * </PRE> 
	 */
	@ShellMethod(value = "show current template params",
			key = { "show-curr-template-params" })
	public void showCurrTemplateParams(
			) {
		log.info("currTemplateParams:" + InsCommand.currTemplateParams);
	}

	
	/**
	 * sample usage
	 * <PRE>
	 * ins --templateName t1 --dir .
	 * </PRE> 
	 */
	@ShellMethod(value = "insert template snippets from templateName + params(or current params) in current dir",
			key = { "ins", "template-insert" })
	public void insertTemplate(
			@ShellOption(value = {"template"}, arity=1) String templateName
			) {
		TemplatesLookupContext templateLookupCtx = templateDefRegistry.lookupContextForDir(currDir);

		TemplateDef templateDef = templateLookupCtx.get(templateName);
		if (templateDef == null) {
			System.out.println("templateName choices : " + templateLookupCtx.getDefs().keySet());
			// TODO completion with choices?
			String promptTemplateName = promptString("please enter valid templateName", null);
			templateDef = templateLookupCtx.get(promptTemplateName);
			if (templateDef == null) {
				//uiOutput.error(uiOutput.err(), "uiContext.attrs:" + uiContextAttributeMap);
				String errorMsg = "Invalid templateName:" + templateName;
				log.error(errorMsg
						+ ", currDir:" + currDir.getAbsolutePath() 
					+ " templateLookupCtx.defs.name: " + templateLookupCtx.getDefs().keySet()
					);
				throw new IllegalArgumentException(errorMsg);
			}
		}

		Map<Object, Object> params = new HashMap<>();
		params.putAll(currTemplateParams);
		for(val paramDef: templateDef.paramDefs) {
			val paramName = paramDef.name;
			val foundParamValue = currTemplateParams.get(paramName);
			if (foundParamValue != null) {
				// may prompt?
				log.info("using paramName:" + foundParamValue);
				params.put(paramName, foundParamValue);
			} else {
				String promptParamValue = promptString(paramName, paramDef.defaultValue);
				params.put(paramName, promptParamValue);
			}
		}
		
		InsertTemplateResult insertRes = templateDef.insertTemplate(currDir, params);

		log.info("Done "+ insertRes);
	}
	
	// ------------------------------------------------------------------------
	
	protected String promptString(String name, String defaultValue) {
		StringInput component = new StringInput(getTerminal(), name, defaultValue);
		component.setResourceLoader(getResourceLoader());
		component.setTemplateExecutor(getTemplateExecutor());
		StringInputContext context = component.run(StringInputContext.empty());
		return context.getResultValue();
	}
	
}
