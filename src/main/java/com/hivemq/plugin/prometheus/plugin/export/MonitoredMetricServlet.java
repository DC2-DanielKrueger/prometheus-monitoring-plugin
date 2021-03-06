/*
 * Copyright 2017 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.plugin.prometheus.plugin.export;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * This class extends the MetricServlet by measuring the duration of the get-method and adds it to the MetricRegistry.
 *
 * @author Daniel Krüger
 */
class MonitoredMetricServlet extends MetricsServlet {

    public static final String metricTopic = "get.time";
    private static final Logger log = LoggerFactory.getLogger(MonitoredMetricServlet.class);

    private final MetricRegistry metricRegistry;
    private final Timer responses;
    private final CollectorRegistry registry;

    MonitoredMetricServlet(CollectorRegistry registry, MetricRegistry metricRegistry) {
        super(registry);
        this.registry = registry;
        this.metricRegistry = metricRegistry;
        this.responses = metricRegistry.timer(name(MonitoredMetricServlet.class, metricTopic));
    }


    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        log.debug("Received HTTP-Get-Request from Prometheus to scrape metrics from HiveMQ.", req.toString());
        final Timer.Context context = responses.time();
        super.doGet(req, resp);
        context.stop();
    }


}
