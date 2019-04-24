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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import org.opennms.netmgt.grafana.model.Dashboard;
import org.opennms.netmgt.grafana.model.DashboardWithMeta;
import org.opennms.netmgt.grafana.model.Panel;

import com.google.gson.Gson;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GrafanaClient {
    private final GrafanaServerConfiguration grafanaServerConfiguration;

    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient();
    private final HttpUrl baseUrl;

    public GrafanaClient(GrafanaServerConfiguration grafanaServerConfiguration) {
        this.grafanaServerConfiguration = Objects.requireNonNull(grafanaServerConfiguration);
        baseUrl = HttpUrl.parse(grafanaServerConfiguration.getUrl());
    }

    public Dashboard getDashboardByUid(String uid) throws IOException {
        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("dashboards")
                .addPathSegment("uid")
                .addPathSegment(uid)
                .build();

        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + grafanaServerConfiguration.getApiKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.body().string());
            }
            final String json = response.body().string();
            final DashboardWithMeta dashboardWithMeta = gson.fromJson(json, DashboardWithMeta.class);
            return dashboardWithMeta.getDashboard();
        }
    }

    public byte[] renderPngForPanel(Dashboard dashboard, Panel panel, int width, int height, long from, long to, Map<String, String> variables) throws IOException {
        final HttpUrl.Builder builder = baseUrl.newBuilder()
                .addPathSegment("render")
                .addPathSegment("d-solo")
                .addPathSegment("uid")
                .addPathSegment(dashboard.getUid())
                .addPathSegments("flow"); //FIXME: What should this be?

        // Query parameters
        builder.addQueryParameter("panelId", Integer.toString(panel.getId()))
                .addQueryParameter("from", Long.toString(from))
                .addQueryParameter("to", Long.toString(to))
                .addQueryParameter("width", Integer.toString(width))
                .addQueryParameter("height", Integer.toString(height));
        variables.forEach((k,v) -> builder.addQueryParameter("var-"+ k, v));

        final Request request = new Request.Builder()
                .url(builder.build())
                .addHeader("Authorization", "Bearer " + grafanaServerConfiguration.getApiKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            try (InputStream is = response.body().byteStream()) {
                return inputStreamToByteArray(is);
            }
        }
    }

    private byte[] inputStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

}
