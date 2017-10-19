package com.example.demo;

import java.time.Duration;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Controller
public class TestApplication {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Compiler compiler;

	@GetMapping("/")
	@ResponseBody
	Mono<Void> home(Model model, ServerWebExchange exchange) throws Exception {
		model.addAttribute("value",
				FluxWriter.wrap(Flux.just("<h2>Demo</h2>", "<span>Hello</span>")
						.delayElements(Duration.ofSeconds(1))));
		ReactiveMustacheView view = new ReactiveMustacheView();
		view.setApplicationContext(context);
		view.setCompiler(compiler);
		view.setUrl("classpath:/templates/home.mustache");
		view.afterPropertiesSet();
		return view.render(model.asMap(), null, exchange);
	}

	@GetMapping("/flux")
	@ResponseBody
	Mono<Void> flux(ServerWebExchange exchange) throws Exception {
		return exchange.getResponse()
				.writeAndFlushWith(Flux
						.just("<html>\n<body>\n", "<h2>Demo</h2>", "<span>Hello</span>\n",
								"</body></html>\n")
						.delayElements(Duration.ofSeconds(1))
						.map(body -> Mono.just(buffer(exchange).write(body.getBytes()))));
	}

	private DataBuffer buffer(ServerWebExchange exchange) {
		return exchange.getResponse().bufferFactory().allocateBuffer();
	}

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}
}
