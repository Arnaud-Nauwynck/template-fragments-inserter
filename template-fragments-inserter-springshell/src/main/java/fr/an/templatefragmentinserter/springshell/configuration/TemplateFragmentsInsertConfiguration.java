package fr.an.templatefragmentinserter.springshell.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.an.templatefragmentinserter.model.TemplateDefRegistry;
import lombok.val;

@Configuration
public class TemplateFragmentsInsertConfiguration {

	@Bean
	public TemplateDefRegistry templateDefRegistry() {
		val res = new TemplateDefRegistry();
		// res.init();
		return res;
	}

}
