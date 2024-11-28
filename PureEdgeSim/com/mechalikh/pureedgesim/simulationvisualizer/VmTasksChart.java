/**
 * 
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

import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Represents a chart showing CPU usage over time for different computing
 * resources.
 */
public class VmTasksChart extends Chart {
        
    protected ArrayList<Map<String, Double>> VmEdgeTasksData = new ArrayList<>();
    protected ArrayList<Map<String, Double>> VmCloudTasksData = new ArrayList<>();

	/**
	 * Constructs a CPUChart object.
	 *
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the x-axis
	 * @param yAxisTitle        the title of the y-axis
	 * @param simulationManager the simulation manager to get data from
	 */
	public VmTasksChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, null, 100.0);
	}

	/**
	 * Updates the chart with the latest CPU usage data.
	 */
	public void update() {
        Map<String, Double> VmEdgeTasksUsage = new HashMap<>();
        Map<String, Double> VmCloudTasksUsage = new HashMap<>();

		VmEdgeTasksUsage(VmEdgeTasksUsage);
		VmCloudTasksUsage(VmCloudTasksUsage);
	}

    protected void VmEdgeTasksUsage(Map<String, Double> VmEdgeTasksUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_ONLY")
        || simulationManager.getScenario().getStringOrchArchitecture().equals("FAR_EDGE_AND_EDGE")
        || simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_AND_CLOUD") 
        || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
		    List<VM> VMedgeOnlyList = new ArrayList<>();

            for(DataCenter DC : computingNodesGenerator.getEdgeOnlyList())
                for(Host host : DC.getHostList())
                    VMedgeOnlyList.addAll(host.getVMList());

            // Get CPU usage for each element in the map
            for (VM vm : VMedgeOnlyList) {
                double num = 100 - vm.getFailureRate();
                VmEdgeTasksUsage.put(vm.getName(), num);
            }

            VmEdgeTasksData.add(VmEdgeTasksUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            int maxDataPoints = (int) (1000 / SimulationParameters.chartsUpdateInterval);
            while (VmEdgeTasksData.size() > maxDataPoints) {
                VmEdgeTasksData.remove(0);
            }

            // Compute the time values for the data points.
            double[] time = new double[VmEdgeTasksData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = VmEdgeTasksData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((VmEdgeTasksData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            updateSize(currentTime - 1000, currentTime, null, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : VmEdgeTasksUsage.entrySet()) {
                char lastChar = entry.getKey().charAt(entry.getKey().length() - 1);
                int identifier = Character.getNumericValue(lastChar);

                // Genera un colore basato sull'ultimo carattere (numero identificatore)
                Color color = getColorBasedOnIdentifier(identifier, Color.BLUE);
                if(simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_ONLY"))
                    updateSeries(getChart(), entry.getKey(), time, toArray(getTasksDataFor(entry.getKey(), VmEdgeTasksData)), SeriesMarkers.NONE, Color.BLACK);
                else updateSeries(getChart(), entry.getKey(), time, toArray(getTasksDataFor(entry.getKey(), VmEdgeTasksData)), SeriesMarkers.NONE, color);
            }
		}
	}

    protected void VmCloudTasksUsage(Map<String, Double> VmCloudTasksUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().equals("CLOUD_ONLY") 
        || simulationManager.getScenario().getStringOrchArchitecture().equals("FAR_EDGE_AND_CLOUD")
        || simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_AND_CLOUD") 
        || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
            
            List<VM> VMcloudOnlyList = new ArrayList<>();
            for(DataCenter DC : computingNodesGenerator.getCloudOnlyList())
                for(Host host : DC.getHostList())
                    VMcloudOnlyList.addAll(host.getVMList());

            // Get CPU usage for each element in the map
            for (VM vm : VMcloudOnlyList) {
                double num = 100 - vm.getFailureRate();
                VmCloudTasksUsage.put(vm.getName(), num);
            }

            VmCloudTasksData.add(VmCloudTasksUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            // int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
            // while (VmCloudCpuUsageData.size() > maxDataPoints) {
            //     VmCloudCpuUsageData.remove(0);
            // }

            // Compute the time values for the data points.
            double[] time = new double[VmCloudTasksData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = VmCloudTasksData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((VmCloudTasksData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            //updateSize(currentTime - 200, currentTime, 0.0, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : VmCloudTasksUsage.entrySet()) {
                char lastChar = entry.getKey().charAt(entry.getKey().length() - 1);
                int identifier = Character.getNumericValue(lastChar);

                // Genera un colore basato sull'ultimo carattere (numero identificatore)
                Color color = getColorBasedOnIdentifier(identifier, Color.GREEN);
                updateSeries(getChart(), entry.getKey(), time, toArray(getTasksDataFor(entry.getKey(), VmCloudTasksData)), SeriesMarkers.NONE, color);
            }
		}
	}
    
    // Utility method to retrieve UpData for a specific key in the map.
    private List<Double> getTasksDataFor(String key, ArrayList<Map<String, Double>> arraylist) {
        List<Double> data = new ArrayList<>();
        for (Map<String, Double> TasksFailed : arraylist) {
            double value = TasksFailed.getOrDefault(key, 0.0);
            data.add(value);
        }
        return data;
    }

    // Metodo per generare un colore basato sull'ultimo carattere della stringa
    private Color getColorBasedOnIdentifier(int identifier, Color baseColor) {
        // Controllo per evitare valori fuori dal range
        if (identifier < 0 || identifier > 9) {
            identifier = 0;  // Valore di default
        }

        // Modifica della tonalità del colore in base all'identificatore
        float[] hsbValues = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float brightness = 0.5f + (identifier * 0.05f); // Modifica la luminosità basandoti sul numero
        return Color.getHSBColor(hsbValues[0], hsbValues[1], Math.min(1.0f, brightness));
    }
    

}