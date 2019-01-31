package com.example.demo;

import java.time.Duration;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Controller
public class TestApplication {

	@Value("${application.delay:1000}")
	private int delay;

	@GetMapping("/")
	String home(Model model) throws Exception {
		model.addAttribute("flux.value",
				Flux.just("<h2>Demo</h2>", "<span>Hello</span>")
						.delayElements(Duration.ofMillis(delay)));
		model.addAttribute("flux.footer",
				Flux.just("World", "Yay!").delayElements(Duration.ofMillis(delay)));
		return "home";
	}

	@GetMapping("/bang")
	String bang(Model model) throws Exception {
		model.addAttribute("flux.value",
				Flux.just("<h2>Demo</h2>", "<span>Hello</span>")
						.delayElements(Duration.ofMillis(delay)));
		// Is it possible to get a 500 from this?
		model.addAttribute("flux.footer", Flux.error(new RuntimeException("bang!")));
		return "home";
	}

	@GetMapping("/flux")
	@ResponseBody
	Mono<Void> flux(ServerWebExchange exchange) throws Exception {
		return exchange.getResponse().writeAndFlushWith(Flux
				.just("<html>\n<body>\n", "<h2>Demo</h2>\n", "<span>Hello</span>\n",
						"</body></html>\n")
				.delayElements(Duration.ofMillis(delay))
				.map(body -> Mono.just(buffer(exchange).write(body.getBytes()))));
	}

	@GetMapping("/bang/flux")
	@ResponseBody
	Mono<Void> bangflux(ServerWebExchange exchange) throws Exception {
		return exchange.getResponse()
				.writeAndFlushWith(Flux
						.just("<html>\n<body>\n", "<h2>Demo</h2>\n",
								"<span>Hello</span>\n", "</body></html>\n")
						.concatWith(Mono.error(new RuntimeException("bang")))
						.delayElements(Duration.ofMillis(delay))
						.map(body -> Mono.just(buffer(exchange).write(body.getBytes()))));
	}

	@GetMapping("/nested")
	@ResponseBody
	Mono<Void> nested(ServerWebExchange exchange) throws Exception {
		return exchange.getResponse()
				.writeAndFlushWith(Flux
						.just(Mono.just("<html>\n<body>\n"),
								Flux.just("<h2>Demo</h2>\n", "<span>Hello</span>\n")
										.delayElements(Duration.ofMillis(delay)),
								Mono.just("</body></html>\n"))
						.flatMap(body -> Flux.from(body).map(
								content -> buffer(exchange).write(content.getBytes())))
						.map(buffer -> Mono.just(buffer)));
	}

	private DataBuffer buffer(ServerWebExchange exchange) {
		return exchange.getResponse().bufferFactory().allocateBuffer();
	}

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}
}

@Configuration
class MustacheCOnfiguration {

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