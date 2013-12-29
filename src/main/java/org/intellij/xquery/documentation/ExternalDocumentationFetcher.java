/*
 * Copyright 2013 Grzegorz Ligas <ligasgr@gmail.com> and other contributors (see the CONTRIBUTORS file).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.xquery.documentation;

import com.intellij.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ligasgr
 * Date: 29/12/13
 * Time: 21:21
 */
public class ExternalDocumentationFetcher {

    private static final Pattern BEGINNING_OF_FUNCTION = Pattern.compile("<(h[0-9])><a name=\"(.*?)\" id=");
    private static final URL EXTERNAL_DOC = ResourceUtil.getResource(
            ExternalDocumentationFetcher.class, "/documentation", "w3c-xpath-functions-30.html");

    public static String fetch(String name) {
        BufferedReader reader = getReader(EXTERNAL_DOC);
        if (reader != null) {
            return retrieveDoc(reader, name);
        } else {
            return null;
        }
    }

    private static BufferedReader getReader(URL url) {
        final InputStreamReader stream;
        try {
            stream = new InputStreamReader(url.openStream());
            return new BufferedReader(stream);
        } catch (IOException e) {
        }
        return null;
    }

    private static String retrieveDoc(BufferedReader reader, String name) {
        try {
            return doRetrieveDoc(reader, name);
        } catch (IOException e) {
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    private static String doRetrieveDoc(BufferedReader reader, String name) throws IOException {
        String line;
        String remainingPart = "";
        boolean functionDocFound = false;
        while ((line = reader.readLine()) != null) {
            if (isDocBegin(line, name)) {
                functionDocFound = true;
                String tagName = getTagName(line);
                if (line.contains("</" + tagName + ">")) {
                    break;
                } else {
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("</" + tagName + ">")) {
                            remainingPart = findAllAfterTagEnd(line, tagName);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (!functionDocFound) {
            return null;
        }
        final StringBuilder builder = new StringBuilder(1024);
        builder.append(remainingPart);
        while ((line = reader.readLine()) != null && !isDocEnd(line)) {
            builder.append(line);
            builder.append("\n");
        }
        return builder.toString();
    }

    private static String findAllAfterTagEnd(String line, String tagName) {
        String closingTag = "</" + tagName + ">";
        return line.substring(line.indexOf(closingTag) + closingTag.length());
    }

    private static boolean isDocEnd(String line) {
        return BEGINNING_OF_FUNCTION.matcher(line).find() || line.startsWith("</html>");
    }

    private static boolean isDocBegin(String line, String name) {
        Matcher matcher = BEGINNING_OF_FUNCTION.matcher(line);
        while (matcher.find()) {
            if (matcher.group(2).equals("func-" + name)) {
                return true;
            }
        }
        return false;
    }

    private static String getTagName(String line) {
        Matcher matcher = BEGINNING_OF_FUNCTION.matcher(line);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
