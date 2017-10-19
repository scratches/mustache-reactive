/*
 * Copyright 2016-2017 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.reactivestreams.Publisher;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

public class FluxWriter extends Writer {

	private final DataBufferFactory factory;
	private final Charset charset;
	private final UnicastProcessor<Publisher<DataBuffer>> emitter;
	private volatile OutputStreamWriter writer;

	public FluxWriter(DataBufferFactory factory, Charset charset) {
		this.factory = factory;
		this.charset = charset;
		this.emitter = UnicastProcessor.<Publisher<DataBuffer>>create();
		start();
	}

	private void start() {
		if (this.writer == null) {
			DataBuffer buffer = this.factory.allocateBuffer();
			this.writer = new OutputStreamWriter(buffer.asOutputStream(), this.charset);
			this.emitter.onNext(Mono.just(buffer));
		}
	}

	public Publisher<? extends Publisher<? extends DataBuffer>> getElements() {
		return emitter;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		start();
		this.writer.write(cbuf, off, len);
	}

	@Override
	public void flush() throws IOException {
		if (writer != null) {
			writer.flush();
			writer.close();
			writer = null;
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		emitter.onComplete();
	}

	public void release() {
		emitter.map(
				p -> Flux.from(p).subscribe(buffer -> DataBufferUtils.release(buffer)));
	}

	public void write(Object object) {
		if (object instanceof Wrapper) {
			Wrapper publisher = (Wrapper) object;
			DataBuffer buffer = this.factory.allocateBuffer();
			emitter.onNext(Flux.from(publisher.publisher)
					.map(string -> buffer.write(string.getBytes(this.charset))));
		}
	}

	public static Wrapper wrap(Publisher<String> publisher) {
		return new Wrapper(publisher);
	}

	static class Wrapper {

		private Publisher<String> publisher;

		public Wrapper(Publisher<String> publisher) {
			this.publisher = publisher;
		}

	}

}