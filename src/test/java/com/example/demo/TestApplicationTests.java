package com.example.demo;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest("application.delay=1")
@AutoConfigureWebTestClient
public class TestApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private MustacheProperties mustache;

	@Test
	public void home() {
		client.get().uri("/").exchange().expectBody(String.class)
				.value(Matchers.containsString("Hello"));
	}

	@Test
	public void sse() {
		client.get().uri("/sse").exchange() //
				.expectBody(String.class).value(Matchers.containsString("event: message"))
				.value(Matchers.containsString("\ndata: <span>Hello</span>"))
				.value(Matchers.containsString("World")).value(Matchers.endsWith("\n\n"));
	}

	@Test
	public void cache() {
		boolean value = mustache.isCache();
		mustache.setCache(true);
		client.get().uri("/").exchange().expectBody(String.class)
				.value(Matchers.containsString("Hello"));
		client.get().uri("/").exchange().expectBody(String.class)
				.value(Matchers.containsString("Hello"));
		mustache.setCache(value);
	}

	@Test
	public void flux() {
		client.get().uri("/flux").exchange().expectBody(String.class)
				.value(Matchers.containsString("Hello"));
	}

	@Test
	public void nested() {
		client.get().uri("/nested").exchange().expectBody(String.class)
				.value(Matchers.containsString("Hello"));
	}

}
