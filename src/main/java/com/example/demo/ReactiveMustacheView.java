/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import com.samskivert.mustache.Template.Fragment;

import org.reactivestreams.Publisher;

import org.springframework.boot.web.reactive.result.view.MustacheView;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring WebFlux {@link View} using the Mustache template engine.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class ReactiveMustacheView extends MustacheView {

	private Compiler compiler;

	private String charset;

	/**
	 * Set the JMustache compiler to be used by this view. Typically this property is not
	 * set directly. Instead a single {@link Compiler} is expected in the Spring
	 * application context which is used to compile Mustache templates.
	 * @param compiler the Mustache compiler
	 */
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * Set the charset used for reading Mustache template files.
	 * @param charset the charset to use for reading template files
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		return resolveResource() != null;
	}

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType,
			ServerWebExchange exchange) {
		Resource resource = resolveResource();
		if (resource == null) {
			return Mono.error(new IllegalStateException(
					"Could not find Mustache template with URL [" + getUrl() + "]"));
		}
		boolean sse = MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType);
		Charset charset = getCharset(contentType).orElse(getDefaultCharset());
		try (FluxWriter writer = new FluxWriter(
				() -> exchange.getResponse().bufferFactory().allocateBuffer(), charset)) {
			try (Reader reader = getReader(resource, sse)) {
				Template template = this.compiler.compile(reader);
				template.execute(model, writer);
				return exchange.getResponse()
						.writeAndFlushWith(Flux.from(writer.getBuffers()));
			}
			catch (Exception ex) {
				writer.release();
				return Mono.error(ex);
			}
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
	}

	@Override
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model) {
		Map<String, Object> result = new HashMap<>();
		for (String key : model.keySet()) {
			if (!key.startsWith("flux") && !key.startsWith("mono")
					&& !key.startsWith("publisher")) {
				result.put(key, model.get(key));
			}
			else {
				model.put(key, new FluxLambda((Publisher<?>) model.get(key)));
			}
		}
		return super.resolveAsyncAttributes(result)
				.doOnSuccess(v -> model.putAll(result));
	}

	private Resource resolveResource() {
		Resource resource = getApplicationContext().getResource(getUrl());
		if (resource == null || !resource.exists()) {
			return null;
		}
		return resource;
	}

	private Reader getReader(Resource resource, boolean sse) throws IOException {
		Reader result;
		if (this.charset != null) {
			result = new InputStreamReader(resource.getInputStream(), this.charset);
		}
		else {
			result = new InputStreamReader(resource.getInputStream());
		}
		if (sse) {
			Reader unclosed = sse(result);
			result = unclosed;
		}
		return result;
	}

	private Reader sse(Reader result) {
		return new SseTemplateReader(result);
	}

	private Optional<Charset> getCharset(MediaType mediaType) {
		return Optional.ofNullable(mediaType != null ? mediaType.getCharset() : null);
	}

	private static class FluxLambda implements Mustache.Lambda {

		private Publisher<?> publisher;

		public FluxLambda(Publisher<?> publisher) {
			this.publisher = publisher;
		}

		@Override
		public void execute(Fragment frag, Writer out) throws IOException {
			if (out instanceof FluxWriter) {
				FluxWriter fluxWriter = (FluxWriter) out;
				fluxWriter.flush();
				fluxWriter.write(Flux.from(publisher).map(value -> frag.execute(value)));
			}
		}

	}

}

class SseTemplateReader extends BufferedReader {

	public SseTemplateReader(Reader in) {
		super(new SseReader(in));
	}

	static class SseReader extends Reader {

		private char[] line;
		private String flux;
		int pointer;
		private BufferedReader origin;

		public SseReader(Reader in) {
			this.origin = new BufferedReader(in);
		}

		@Override
		public void close() throws IOException {
			origin.close();
		}

		public int read(char cbuf[], int off, int len) throws IOException {
			int total = 0;
			if (line == null) {
				line = next();
			}
			int size = len;
			while (line != null) {
				if (size >= line.length - pointer) {
					System.arraycopy(line, pointer, cbuf, off, line.length - pointer);
					size -= line.length - pointer;
					total += line.length - pointer;
					off += line.length - pointer;
					line = next();
					pointer = 0;
				}
				else {
					if (size > 0) {
						System.arraycopy(line, pointer, cbuf, off, size);
						total += size;
						pointer += size;
					}
					// we wrote the whole buffer
					break;
				}
			}
			return total == 0 && line==null ? -1 : total;
		}

		private char[] next() throws IOException {
			String line = origin.readLine();
			if (this.line == null) {
				while (line != null && line.trim().length() == 0) {
					// first non-empty line
					line = origin.readLine();
				}
				if (line != null) {
					// first line
					this.flux = line;
					line = line + "\nevent: message\nid: {{id}}\n";
				}
			}
			else if (flux != null && line != null
					&& line.trim().equals(flux.replace("#", "/"))) {
				// last line has 2 empty lines before it
				line = "\n\n" + line + "\n";
			}
			else if (line != null) {
				line = "data: " + line + "\n";
			}
			return line == null ? null : line.toCharArray();
		}
	}

}