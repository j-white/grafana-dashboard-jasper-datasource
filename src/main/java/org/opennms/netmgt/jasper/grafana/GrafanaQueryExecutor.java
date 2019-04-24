/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
import java.util.Map;

import org.opennms.netmgt.grafana.GrafanaClient;
import org.opennms.netmgt.grafana.GrafanaServerConfiguration;
import org.opennms.netmgt.grafana.model.Dashboard;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

public class GrafanaQueryExecutor extends JRAbstractQueryExecuter {

    public GrafanaQueryExecutor(JasperReportsContext context, JRDataset dataset, Map<String,? extends JRValueParameter> parameters) {
        super(context, dataset, parameters);
    }

    @Override
    protected String getParameterReplacement(String parameterName) {
        return String.valueOf(getParameterValue(parameterName));
    }

    @Override
    public JRDataSource createDatasource() throws JRException {
        final GrafanaServerConfiguration config = GrafanaServerConfiguration.fromEnv();
        final GrafanaClient client = new GrafanaClient(config);
        final Dashboard dashboard;
        try {
            dashboard = client.getDashboardByUid("eWsVEL6zz");
        } catch (IOException e) {
            throw new JRException(e);
        }
        return new GrafanaPanelDatasource(client, dashboard);
    }

    @Override
    public void close() {
        // pass
    }

    @Override
    public boolean cancelQuery() {
        return false;
    }
}
