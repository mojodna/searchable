/*
 Copyright 2006 Seth Fitzsimmons <seth@mojodna.net>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package net.mojodna.searchable.util;

import java.lang.annotation.Annotation;

/**
 * Compound key for use with annotation caches.
 * 
 * @author Seth Fitzsimmons
 */
class AnnotationKey {
	private Class<? extends Annotation> annotationClass;

	private Object classOrMethod;

	/**
	 * Constructor.
	 * 
	 * @param classOrMethod Class or method used as half of the compound key.
	 * @param annotationClass Annotation class.
	 */
	public AnnotationKey(final Object classOrMethod,
			final Class<? extends Annotation> annotationClass) {
		this.classOrMethod = classOrMethod;
		this.annotationClass = annotationClass;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AnnotationKey))
			return false;

		AnnotationKey key = (AnnotationKey) obj;

		return classOrMethod.equals(key.classOrMethod)
				&& annotationClass.equals(key.annotationClass);
	}

	@Override
	public int hashCode() {
		return 65521 * classOrMethod.hashCode() + annotationClass.hashCode();
	}
}
