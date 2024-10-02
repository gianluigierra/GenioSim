package com.mechalikh.pureedgesim.scenariomanager;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

//MODIFICA MIA, ho modificato praticamente tutto il file per effettuare il corretto parsing degli EdgeDC e del Cloud con le VM
public class DatacentersParser extends ComputingNodesParser {

	public DatacentersParser(String file, TYPES type) {
		super(file, type);
	}

	@Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList datacenterList = xmlDoc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			for (String element : List.of("isOrchestrator", "idleConsumption", "maxConsumption"))
				isElementPresent(datacenterElement, element);

			assertDouble(datacenterElement, "idleConsumption", value -> (value >= 0),
					">= 0. Check the file " + file);
			double idleConsumption = Double
					.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
			assertDouble(datacenterElement, "maxConsumption", value -> (value > idleConsumption),
					"> \"idleConsumption\". Check the file " + file);

			isElementPresent(datacenterElement, "hosts");
			isElementPresent(datacenterElement, "host");
			NodeList hostList = datacenterElement.getElementsByTagName("host");
			for (int j = 0; j < hostList.getLength(); j++) {
				Node hostNode = hostList.item(j);

				Element hostElement = (Element) hostNode;

				
				for (String element : List.of("cores", "mips", "ram", "storage"))
					isElementPresent(datacenterElement, element);

				for (String element : List.of("cores", "mips", "ram", "storage"))
					assertDouble(datacenterElement, element, value -> (value > 0), "> 0. Check the file: " + file);

				double Hostcores = Double.parseDouble(datacenterElement.getElementsByTagName("cores").item(0).getTextContent());
				double Hostmips = Double.parseDouble(datacenterElement.getElementsByTagName("mips").item(0).getTextContent());
				double Hostram = Double.parseDouble(datacenterElement.getElementsByTagName("ram").item(0).getTextContent());
				double Hoststorage = Double.parseDouble(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());

				double cores = 0;
				double mips = 0;
				double ram = 0;
				double storage = 0;
				
				NodeList vmList = hostElement.getElementsByTagName("VM");
				for (int k = 0; k < vmList.getLength(); k++) {
					Node vmNode = vmList.item(k);

					Element vmElement = (Element) vmNode;
					isElementPresent(vmElement, "cores");
					isElementPresent(vmElement, "mips");
					isElementPresent(vmElement, "ram");
					isElementPresent(vmElement, "storage");

					cores += Double.parseDouble(vmElement.getElementsByTagName("cores").item(0).getTextContent());
					mips = Double.parseDouble(vmElement.getElementsByTagName("mips").item(0).getTextContent());
					if(Hostmips < mips)  throw new IllegalArgumentException( getClass().getSimpleName() + " - Error, the mips of VMs must be lower-equal than the mips of the Host!\"");
					ram += Double.parseDouble(vmElement.getElementsByTagName("ram").item(0).getTextContent());
					storage += Double.parseDouble(vmElement.getElementsByTagName("storage").item(0).getTextContent());

				}

				if(Hostcores < cores) throw new IllegalArgumentException( getClass().getSimpleName() + " - Error, the sum of cores of VMs must be lower-equal than the cores of the Host!");
				if(Hostram < ram)  throw new IllegalArgumentException( getClass().getSimpleName() + " - Error, the sum of ram of VMs must be lower-equal than the ram of the Host!\"");
				if(Hoststorage < storage)  throw new IllegalArgumentException( getClass().getSimpleName() + " - Error, the sum of storage of VMs must be lower-equal than the storage of the Host!");

			}

			if (type == TYPES.CLOUD) {
				SimulationParameters.numberOfCloudDataCenters++;
				Element location = (Element) datacenterElement.getElementsByTagName("location").item(0); 		// MODIFICA MIA, non erano presenti queste info sul cloud
				isElementPresent(location, "x_pos"); 																	// originariamente la posizione non era necessaria sul cloud
				isElementPresent(location, "y_pos"); 																	// Luciano però l'ha aggiunta, quindi la parsizzo
				assertDouble(location, "x_pos", value -> (value >= 0), ">= 0. Check the " + file + " file!"); 	//
				assertDouble(location, "y_pos", value -> (value > 0), "> 0. Check the " + file + " file!"); 		//
			} else if (type == TYPES.EDGE_DATACENTER) { 																	// MODIFICA MIA, qui non era presente l'if. Era solo else
				SimulationParameters.numberOfEdgeDataCenters++;
				Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");
				assertDouble(location, "x_pos", value -> (value >= 0), ">= 0. Check the " + file + " file!");
				assertDouble(location, "y_pos", value -> (value > 0), "> 0. Check the " + file + " file!");
			}

		}
		return true;
	}

}
