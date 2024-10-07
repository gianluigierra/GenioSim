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
public class VmCPUChart extends Chart {
        
    protected ArrayList<Map<String, Double>> VmEdgeCpuUsageData = new ArrayList<>();
    protected ArrayList<Map<String, Double>> VmCloudCpuUsageData = new ArrayList<>();

	/**
	 * Constructs a CPUChart object.
	 *
	 * @param title             the title of the chart
	 * @param xAxisTitle        the title of the x-axis
	 * @param yAxisTitle        the title of the y-axis
	 * @param simulationManager the simulation manager to get data from
	 */
	public VmCPUChart(String title, String xAxisTitle, String yAxisTitle, SimulationManager simulationManager) {
		super(title, xAxisTitle, yAxisTitle, simulationManager);
		getChart().getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		updateSize(0.0, null, 0.0, null);
	}

	/**
	 * Updates the chart with the latest CPU usage data.
	 */
	public void update() {
        Map<String, Double> VmEdgeCpuUsage = new HashMap<>();
        Map<String, Double> VmCloudCpuUsage = new HashMap<>();

		VmEdgeCpuUsage(VmEdgeCpuUsage);
		VmCloudCpuUsage(VmCloudCpuUsage);
	}

    protected void VmEdgeCpuUsage(Map<String, Double> VmEdgeCpuUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().contains("EDGE") || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
		    List<VM> VMedgeOnlyList = new ArrayList<>();

            for(DataCenter DC : computingNodesGenerator.getEdgeOnlyList())
                for(Host host : DC.getHostList())
                    VMedgeOnlyList.addAll(host.getVMList());

            // Get CPU usage for each element in the map
            for (VM vm : VMedgeOnlyList) {
                VmEdgeCpuUsage.put(vm.getName(), vm.getAvgCpuUtilization());
            }

            VmEdgeCpuUsageData.add(VmEdgeCpuUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            // int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
            // while (VmEdgeCpuUsageData.size() > maxDataPoints) {
            //     VmEdgeCpuUsageData.remove(0);
            // }

            // Compute the time values for the data points.
            double[] time = new double[VmEdgeCpuUsageData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = VmEdgeCpuUsageData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((VmEdgeCpuUsageData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            //updateSize(currentTime - 200, currentTime, 0.0, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : VmEdgeCpuUsage.entrySet()) {
                char lastChar = entry.getKey().charAt(entry.getKey().length() - 1);
                int identifier = Character.getNumericValue(lastChar);

                // Genera un colore basato sull'ultimo carattere (numero identificatore)
                Color color = getColorBasedOnIdentifier(identifier, Color.BLUE);
                if(simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_ONLY"))
                    updateSeries(getChart(), entry.getKey(), time, toArray(getCPUDataFor(entry.getKey(), VmEdgeCpuUsageData)), SeriesMarkers.NONE, Color.BLACK);
                else updateSeries(getChart(), entry.getKey(), time, toArray(getCPUDataFor(entry.getKey(), VmEdgeCpuUsageData)), SeriesMarkers.NONE, color);
            }
		}
	}

    protected void VmCloudCpuUsage(Map<String, Double> VmCloudCpuUsage) {

        if (simulationManager.getScenario().getStringOrchArchitecture().contains("CLOUD") || simulationManager.getScenario().getStringOrchArchitecture().equals("ALL")) {
            
            List<VM> VMcloudOnlyList = new ArrayList<>();
            for(DataCenter DC : computingNodesGenerator.getCloudOnlyList())
                for(Host host : DC.getHostList())
                    VMcloudOnlyList.addAll(host.getVMList());

            // Get CPU usage for each element in the map
            for (VM vm : VMcloudOnlyList) {
                VmCloudCpuUsage.put(vm.getName(), vm.getAvgCpuUtilization());
            }

            VmCloudCpuUsageData.add(VmCloudCpuUsage);

            //Se decommento questo devo decommentare anche sotto "update the chart with new data" in quanto questi mi visualizzano solo le percentuali recenti di Cpu
            // Remove old data points.
            // int maxDataPoints = (int) (300 / SimulationParameters.chartsUpdateInterval);
            // while (VmCloudCpuUsageData.size() > maxDataPoints) {
            //     VmCloudCpuUsageData.remove(0);
            // }

            // Compute the time values for the data points.
            double[] time = new double[VmCloudCpuUsageData.size()];
            double currentTime = simulationManager.getSimulation().clock();
            for (int i = VmCloudCpuUsageData.size() - 1; i >= 0; i--) {
                time[i] = currentTime - ((VmCloudCpuUsageData.size() - i) * SimulationParameters.chartsUpdateInterval);
            }

            // Update the chart with the new data.
            //updateSize(currentTime - 200, currentTime, 0.0, null);

            // Update the series for each element in the map.
            for (Map.Entry<String, Double> entry : VmCloudCpuUsage.entrySet()) {
                char lastChar = entry.getKey().charAt(entry.getKey().length() - 1);
                int identifier = Character.getNumericValue(lastChar);

                // Genera un colore basato sull'ultimo carattere (numero identificatore)
                Color color = getColorBasedOnIdentifier(identifier, Color.GREEN);
                updateSeries(getChart(), entry.getKey(), time, toArray(getCPUDataFor(entry.getKey(), VmCloudCpuUsageData)), SeriesMarkers.NONE, color);
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