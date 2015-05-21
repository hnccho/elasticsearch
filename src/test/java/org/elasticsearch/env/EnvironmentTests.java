/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.env;

import com.google.common.base.Charsets;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * Simple unit-tests for Environment.java
 */
public class EnvironmentTests extends ElasticsearchTestCase {

    public Environment newEnvironment() throws IOException {
        return newEnvironment(ImmutableSettings.EMPTY);
    }

    public Environment newEnvironment(Settings settings) throws IOException {
        Settings build = ImmutableSettings.builder()
                .put(settings)
                .put("path.home", createTempDir().toAbsolutePath())
                .putArray("path.data", tmpPaths()).build();
        return new Environment(build);
    }

    @Test
    public void testResolveJaredResource() throws IOException {
        Environment environment = newEnvironment();
        URL url = environment.resolveConfig("META-INF/MANIFEST.MF"); // this works because there is one jar having this file in the classpath
        assertNotNull(url);
        try (BufferedReader reader = FileSystemUtils.newBufferedReader(url, Charsets.UTF_8)) {
            String string = Streams.copyToString(reader);
            assertTrue(string, string.contains("Manifest-Version"));
        }
    }

    @Test
    public void testResolveFileResource() throws IOException {
        Environment environment = newEnvironment();
        URL url = environment.resolveConfig("org/elasticsearch/common/cli/tool.help");
        assertNotNull(url);
        try (BufferedReader reader = FileSystemUtils.newBufferedReader(url, Charsets.UTF_8)) {
            String string = Streams.copyToString(reader);
            assertEquals(string, "tool help");
        }
    }

    @Test
    public void testRepositoryResolution() throws IOException {
        Environment environment = newEnvironment();
        assertThat(environment.resolveRepoFile("/test/repos/repo1"), nullValue());
        assertThat(environment.resolveRepoFile("test/repos/repo1"), nullValue());
        environment = newEnvironment(settingsBuilder().putArray("path.repo", "/test/repos", "/another/repos").build());
        assertThat(environment.resolveRepoFile("/test/repos/repo1"), notNullValue());
        assertThat(environment.resolveRepoFile("test/repos/repo1"), notNullValue());
        assertThat(environment.resolveRepoFile("/another/repos/repo1"), notNullValue());
        assertThat(environment.resolveRepoFile("/test/repos/../repo1"), nullValue());
        assertThat(environment.resolveRepoFile("/test/repos/../repos/repo1"), notNullValue());
        assertThat(environment.resolveRepoFile("/somethingeles/repos/repo1"), nullValue());
    }

}
