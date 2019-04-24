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

package org.opennms.netmgt.jasper.grafana;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.opennms.netmgt.grafana.GrafanaClient;
import org.opennms.netmgt.grafana.model.Dashboard;
import org.opennms.netmgt.grafana.model.Panel;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

public class GrafanaPanelDatasource implements JRRewindableDataSource {
    public static final String IMAGE_FIELD_NAME = "png";
    public static final String WIDTH_FIELD_NAME = "width";
    public static final String HEIGHT_FIELD_NAME = "height";

    private final GrafanaClient client;
    private final Dashboard dashboard;

    // state
    private Iterator<Panel> iterator;
    private Panel currentPanel;

    public GrafanaPanelDatasource(GrafanaClient client, Dashboard dashboard) {
        this.client = Objects.requireNonNull(client);
        this.dashboard = Objects.requireNonNull(dashboard);
        moveFirst();
    }

    @Override
    public void moveFirst() {
        iterator = dashboard.getPanels().stream()
                // Dont' render rows
                .filter(p -> !Objects.equals("row", p.getType()))
                .collect(Collectors.toList())
                .iterator();
    }

    @Override
    public boolean next() {
        if (!iterator.hasNext()) {
            return false;
        }
        currentPanel = iterator.next();
        return true;
    }

    @Override
    public Object getFieldValue(JRField jrField) throws JRException {
        int width = 1000;
        int height = 500;
        Map<String, String> variables = new HashMap<>();
        variables.put("node", "1");
        variables.put("interface", "2");

        if (Objects.equals(WIDTH_FIELD_NAME, jrField.getName())) {
            return width;
        } else if (Objects.equals(HEIGHT_FIELD_NAME, jrField.getName())) {
            return height;
        } else if (Objects.equals(IMAGE_FIELD_NAME, jrField.getName())) {
            try {
                return client.renderPngForPanel(dashboard, currentPanel, width, height,
                        System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), System.currentTimeMillis(), variables);
            } catch (IOException e) {
                throw new JRException(e);
            }
        }
        return null;
    }

}
