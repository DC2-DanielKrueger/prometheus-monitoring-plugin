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

package com.hivemq.plugin.prometheus.plugin.configuration;

import com.hivemq.plugin.prometheus.plugin.exception.InvalidConfigurationException;
import com.hivemq.spi.config.SystemInformation;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides the possibility to obtain the configuration of the prometheus-plugin via readConfiguration()
 *
 * @author Daniel Krüger
 */
public class ConfigurationReader {


    private static final Logger log = LoggerFactory.getLogger(ConfigurationReader.class);
    /**
     * The path of the prometheusConfiguration.properties file relative to the HiveMQ conf folder
     */
    public static final String CONFIG_PATH = "prometheusConfiguration.properties";
    /**
     * The minimal possible  port
     */
    public static final int MIN_PORT = 1;
    /**
     * The maximum possible port
     */
    public static final int MAX_PORT = 65535;


    private final SystemInformation systemInformation;

    /**
     * Constructs a new ConfigurationReader object. The {@link SystemInformation} is injected via Guice
     *
     * @param systemInformation is injected via Guice
     */
    @Inject
    public ConfigurationReader(SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }


    /**
     * @return the {@link PrometheusPluginConfiguration} which holds the configuration-information
     * @throws FileNotFoundException         if the prometheusConfiguration.properties is not found in the conf folder of HiveMQ
     * @throws InvalidConfigurationException if the configuration cannot be initiated due to other critical errors
     * @throws Exception                     if the configuration can not be initialized correctly due to unknown exception
     */
    public PrometheusPluginConfiguration readConfiguration() throws Exception {
        final File file = new File(systemInformation.getConfigFolder(), CONFIG_PATH);


        if (!file.canRead()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }


        try (FileInputStream in = new FileInputStream(file)) {
            final Properties properties = new Properties();
            properties.load(in);

            testAllPropertiesDeclared(properties);

            PrometheusPluginConfiguration prometheusPluginConfiguration = ConfigFactory.create(PrometheusPluginConfiguration.class, properties);

            testConfiguration(prometheusPluginConfiguration);
            return prometheusPluginConfiguration;
        } catch (IOException e) {
            throw new InvalidConfigurationException("Error while reading configuration file.");
        }
    }


    /**
     * In the ConfigFactory.create() it is not tested whether the entries make sense (here e.g. port holds a int)
     *
     * @param config config to be tested
     * @throws InvalidConfigurationException thrown when a entry makes no sense or does not meet the requirements
     */
    private void testConfiguration(PrometheusPluginConfiguration config) throws InvalidConfigurationException {
        boolean error = false;
        StringBuilder sb = new StringBuilder();


        //test port
        try {
            testPortSense(config);
        } catch (InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }

        //test MetricPath
        try {
            testMetricsPathSense(config);
        } catch (InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }

        //testIP
        try {
            testIpSense(config);
        } catch (InvalidConfigurationException e) {
            error = true;
            sb.append(e.getMessage());
        }


        if (error) {
            String msg = "Error while parsing and testing the configuration: " + sb.toString();
            throw new InvalidConfigurationException(msg);
        }
    }

    private void testPortSense(PrometheusPluginConfiguration config) throws InvalidConfigurationException {
        try {
            config.port();
        } catch (Exception e) {
            throw new InvalidConfigurationException("Invalid port configuration");
        }
        int port = config.port();

        if (port < MIN_PORT) {
            throw new InvalidConfigurationException("The port must not be smaller than " + MIN_PORT + "." + " Value was " + port + ".");
        }

        if (port > MAX_PORT) {
            throw new InvalidConfigurationException("The port must not be greater than " + MAX_PORT + "." + " Value was " + port + ".");
        }
    }

    private void testIpSense(PrometheusPluginConfiguration config) throws InvalidConfigurationException {
        try {
            config.hostIp();
        } catch (Exception e) {
            throw new InvalidConfigurationException("Invalid host ip configuration.");
        }


        String ip = config.hostIp();
        if (ip.trim().length() == 0) {
            throw new InvalidConfigurationException("The ip must not be blank.");
        }


    }

    private void testMetricsPathSense(PrometheusPluginConfiguration config) throws InvalidConfigurationException {
        try {
            config.metricPath();
        } catch (Exception e) {
            throw new InvalidConfigurationException("Invalid metric_path configuration.");
        }


        String path = config.metricPath();
        if (!path.startsWith("/")) {
            throw new InvalidConfigurationException("The metric_path must begin with a slash, f.e. \"/metrics\".");
        }
    }


    private void testAllPropertiesDeclared(Properties properties) throws InvalidConfigurationException {
        boolean error = false;
        StringBuilder sb = new StringBuilder();

        if (!properties.containsKey(PrometheusPluginConfiguration.METRIC_PATH_KEY)) {
            sb.append(" " + PrometheusPluginConfiguration.METRIC_PATH_KEY);
            error = true;
        }
        if (!properties.containsKey(PrometheusPluginConfiguration.IP_KEY)) {
            sb.append(" " + PrometheusPluginConfiguration.IP_KEY);
            error = true;
        }
        if (!properties.containsKey(PrometheusPluginConfiguration.PORT_KEY)) {
            sb.append(" " + PrometheusPluginConfiguration.PORT_KEY);
            error = true;
        }
        if (error) {
            String msg = "Missing required configuration of:" + sb.toString() + ".";
            throw new InvalidConfigurationException(msg);
        }
    }


}
