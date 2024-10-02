/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 *     @since PureEdgeSim 2.0
 **/
package com.mechalikh.pureedgesim.simulationvisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.mechalikh.pureedgesim.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Represents a chart showing CPU usage over time for different computing
 * resources.
 */
public class HostCPUChart extends Chart {
        
    protected ArrayList<Map<String, Double>> HostEdgeCpuUsageData = new ArrayList<>();
    protected ArrayList<Map<String, Double>> HostCloudCpuUsageData = new ArrayList<>();

	/**
	 * Constructs a CPUChart object.
	 *
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the x-axis
	 * @param yAxisTitle        the title of the y-axis
	 * @param simulationManager the simulation manager to get data from
	 */
	public HostCPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, 0.0, null);
	}

	/**
	 * Updates the chart with the latest CPU usage data.
	 */
	public void update() {

        Map<String, Double> HostEdgeCpuUsage = new HashMap<>();
        Map<String, Double> HostCloudCpuUsage = new HashMap<>();

		HostEdgeCpuUsage(HostEdgeCpuUsage);
		HostCloudCpuUsage(HostCloudCpuUsage);
	}

    protected void HostEdgeCpuUsage(Map<String, Double> HostEdgeCpuUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE") || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
		    List<Host> HostedgeOnlyList = new ArrayList<>();

            for(DataCenter DC : computingNodesGenerator.getEdgeOnlyList())
                for(Host host : DC.getHostList())
                    HostedgeOnlyList.add(host);

            // Get CPU usage for each element in the map
            for (Host Host : HostedgeOnlyList) {
                HostEdgeCpuUsage.put(Host.getName(), Host.getAvgCpuUtilization());
            }

            HostEdgeCpuUsageData.add(HostEdgeCpuUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            // int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
            // while (HostEdgeCpuUsageData.size() > maxDataPoints) {
            //     HostEdgeCpuUsageData.remove(0);
            // }

            // Compute the time values for the data points.
            double[] time = new double[HostEdgeCpuUsageData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = HostEdgeCpuUsageData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((HostEdgeCpuUsageData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            //updateSize(currentTime - 200, currentTime, 0.0, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : HostEdgeCpuUsage.entrySet()) {
                updateSeries(getChart(), entry.getKey(), time, toArray(getCPUDataFor(entry.getKey(), HostEdgeCpuUsageData)), SeriesMarkers.NONE, Color.BLACK);
            }
		}
	}

    protected void HostCloudCpuUsage(Map<String, Double> HostCloudCpuUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().contains("CLOUD") || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
            
            List<Host> HostcloudOnlyList = new ArrayList<>();
            for(DataCenter DC : computingNodesGenerator.getCloudOnlyList())
                    for(Host host : DC.getHostList())
                    HostcloudOnlyList.add(host);

            // Get CPU usage for each element in the map
            for (Host Host : HostcloudOnlyList) {
                HostCloudCpuUsage.put(Host.getName(), Host.getAvgCpuUtilization());
            }

            HostCloudCpuUsageData.add(HostCloudCpuUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            // int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
            // while (HostCloudCpuUsageData.size() > maxDataPoints) {
            //     HostCloudCpuUsageData.remove(0);
            // }

            // Compute the time values for the data points.
            double[] time = new double[HostCloudCpuUsageData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = HostCloudCpuUsageData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((HostCloudCpuUsageData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            //updateSize(currentTime - 200, currentTime, 0.0, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : HostCloudCpuUsage.entrySet()) {
                updateSeries(getChart(), entry.getKey(), time, toArray(getCPUDataFor(entry.getKey(), HostCloudCpuUsageData)), SeriesMarkers.NONE, Color.BLACK);
            }
		}
	}
    
    // Utility method to retrieve UpData for a specific key in the map.
    private List<Double> getCPUDataFor(String key, ArrayList<Map<String, Double>> arraylist) {
        List<Double> data = new ArrayList<>();
        for (Map<String, Double> CpuUsage : arraylist) {
            double value = CpuUsage.getOrDefault(key, 0.0);
            data.add(value);
        }
        return data;
    }
    
}