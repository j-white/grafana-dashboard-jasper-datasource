/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.grafana;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.netmgt.grafana.model.Dashboard;
import org.opennms.netmgt.grafana.model.Health;
import org.opennms.netmgt.grafana.model.Panel;
import org.opennms.netmgt.grafana.model.PanelContainer;
import org.opennms.netmgt.grafana.model.SearchResult;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class GrafanaClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private GrafanaServerConfiguration getClientConfig() {
        return GrafanaServerConfiguration.builder()
                .withUrl(wireMockRule.baseUrl())
                .withApiKey("xxxx")
                .build();
    }

    @Test
    public void canSearchForDashboards() throws IOException {
        stubFor(get(urlEqualTo("/api/search?type=dash-db&query=abc"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("search.json")));

        // Search for dashboards
        GrafanaClient client = new GrafanaClient(getClientConfig());
        List<SearchResult> results = client.searchForDashboards("abc");
        assertThat(results, hasSize(4));

        // Verify a specific entry
        SearchResult result = results.get(0);
        assertThat(result.getId(), equalTo(14));
        assertThat(result.getUid(), equalTo("QS9VaTFmz"));
        assertThat(result.getTitle(), equalTo("Bamboo"));
        assertThat(result.getUrl(), equalTo("/d/QS9VaTFmz/bamboo"));
        assertThat(result.getType(), equalTo("dash-db"));
        assertThat(result.getTags(), contains("tag1", "tag2"));
        assertThat(result.isStarred(), equalTo(true));
    }

    @Test
    public void canGetDashboardAndRenderPng() throws IOException {
        stubFor(get(urlEqualTo("/api/dashboards/uid/eWsVEL6zz"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("dashboard.json")));

        GrafanaClient client = new GrafanaClient(getClientConfig());
        Dashboard dashboard = client.getDashboardByUid("eWsVEL6zz");

        assertThat(panelTitles(dashboard), contains("Traffic (Flows)", "Traffic by Application",
                "Traffic by Application",
                "Traffic (SNMP via MIB-2)",
                "MIB-2 Traffic",
                "MIB-2 Errors and Discards",
                "Conversation (Flows)"));

        Panel row = dashboard.getPanels().get(6);
        assertThat(panelTitles(row), contains("Traffic by Conversation (Top N)", "Traffic by Conversation (Top N)"));

        Panel panel = dashboard.getPanels().get(1);
        assertThat(panel.getId(), equalTo(3));
        assertThat(panel.getDatasource(), equalTo("minion-dev (Flow)"));
        assertThat(panel.getDescription(), equalTo("igb0"));

        stubFor(get(urlEqualTo("/render/d-solo/eWsVEL6zz/z?panelId=3&from=0&to=1&width=128&height=128&theme=light"))
                .willReturn(aResponse()
                .withHeader("Content-Type", "image/png")
                .withBodyFile("panel.png")));

        byte[] pngBytes = client.renderPngForPanel(dashboard, panel, 128, 128, 0L, 1L, Collections.emptyMap());
        assertThat(pngBytes.length, equalTo(6401));
    }

    @Test
    public void canGetHealth() throws IOException {
        stubFor(get(urlEqualTo("/api/health"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("health.json")));

        GrafanaClient client = new GrafanaClient(getClientConfig());
        Health health = client.getServerHealth();

        assertThat(health.getCommit(), equalTo("5d16da7"));
        assertThat(health.getDatabase(), equalTo("ok"));
        assertThat(health.getVersion(), equalTo("6.2.0"));
    }

    private static List<String> panelTitles(PanelContainer container) {
        return container.getPanels().stream().map(Panel::getTitle).collect(Collectors.toList());
    }
}
