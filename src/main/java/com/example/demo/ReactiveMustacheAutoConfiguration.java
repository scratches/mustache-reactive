package com.example.demo;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;

import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnClass({Mustache.class, Flux.class})
@AutoConfigureBefore(MustacheAutoConfiguration.class)
public class ReactiveMustacheAutoConfiguration {

	@Autowired
	private Compiler compiler;

	@Autowired
	private MustacheProperties mustache;

	@Bean
	public MustacheViewResolver reactiveMustacheViewResolver() {
		MustacheViewResolver resolver = new MustacheViewResolver(compiler) {
			@Override
			protected Class<?> requiredViewClass() {
				return ReactiveMustacheView.class;
			}

			@Override
			protected AbstractUrlBasedView createView(String viewName) {
				ReactiveMustacheView view = (ReactiveMustacheView) super.createView(
						viewName);
				view.setCompiler(compiler);
				view.setCharset(mustache.getCharsetName());
				view.setCache(ReactiveMustacheAutoConfiguration.this.mustache.isCache());
				return view;
			}
		};
		resolver.setPrefix(this.mustache.getPrefix());
		resolver.setSuffix(this.mustache.getSuffix());
		resolver.setViewNames(this.mustache.getViewNames());
		resolver.setRequestContextAttribute(this.mustache.getRequestContextAttribute());
		resolver.setCharset(this.mustache.getCharsetName());
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return resolver;
	}

}