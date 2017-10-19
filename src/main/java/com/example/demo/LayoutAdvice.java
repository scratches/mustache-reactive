/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Map;

import com.samskivert.mustache.Mustache;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class LayoutAdvice {

	@ModelAttribute("flux")
	public Mustache.Lambda flux() {
		return (frag, out) -> {
			if (out instanceof FluxWriter) {
				FluxWriter fluxWriter = (FluxWriter) out;
				fluxWriter.flush();
				String key = frag.decompile();
				if (frag.context() instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> context = (Map<String, Object>) frag.context();
					fluxWriter.write(context.get(key));
				}
			}
		};
	}
}