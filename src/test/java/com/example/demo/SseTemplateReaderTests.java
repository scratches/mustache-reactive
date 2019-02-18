/*
 * Copyright 2018 the original author or authors.
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
import java.io.StringReader;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class SseTemplateReaderTests {

	@Test
	public void test() throws Exception {
		String template = "{{#flux.foo}}\nfoo\nbar\n{{/flux.foo}}";
		SseTemplateReader reader = new SseTemplateReader(new BufferedReader(new StringReader(template)));
		assertThat(reader.readLine()).isEqualTo("{{#flux.foo}}");
		assertThat(reader.readLine()).isEqualTo("event: message");
		assertThat(reader.readLine()).isEqualTo("id: {{id}}");
		assertThat(reader.readLine()).isEqualTo("data: foo");
		assertThat(reader.readLine()).isEqualTo("data: bar");
		assertThat(reader.readLine()).isEqualTo("");
		assertThat(reader.readLine()).isEqualTo("");
		assertThat(reader.readLine()).isEqualTo("{{/flux.foo}}");
		assertThat(reader.readLine()).isNull();
		reader.close();
	}

}
