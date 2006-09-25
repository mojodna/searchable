/*
 Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>

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
package net.mojodna.searchable;

import java.util.Collection;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Utility methods for the Searchable toolkit.
 * 
 * @author Seth Fitzsimmons
 */
public class SearchableUtils {
	/**
	 * Converts a Collection to an array of Strings.
	 * 
	 * @param fields Collection to convert.
	 * @return Array of Strings.
	 */
	public static String[] toStringArray(final Collection fields) {
		final String[] defaultFields = new String[fields.size()];
		int i = 0;
		for (final Object f : fields) {
			defaultFields[i++] = f.toString();
		}
		return defaultFields;
	}

	/**
	 * Is the specified character a reserved Lucene character.
	 *
	 * @param c
	 * @return Whether the specified character is reserved.
	 */
	public static boolean isReservedCharacter(char c) {
		return (c == '+') || (c == '-') || (c == '&') || (c == '|')
				|| (c == '!') || (c == '(') || (c == ')') || (c == '{')
				|| (c == '}') || (c == '[') || (c == ']') || (c == '^')
				|| (c == '"') || (c == '~') || (c == '*') || (c == '?')
				|| (c == ':') || (c == '\\');
	}

	/**
	 * Formats a date for lucene usage. 
	 * @param date the date to be converted to a string.
	 * @return date in YYYYMMDD format
	 */
	public static String simpleDateToString(Date date) {
		SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat
				.getDateInstance();
		dateFormat.applyPattern("yyyyMMdd");
		return dateFormat.format(date);
	}
}
