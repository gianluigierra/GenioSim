package com.mechalikh.pureedgesim.scenariomanager;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

//MODIFICA MIA, questa classe non esisteva. L'ho creata per effetuale il parsing degli ONT
public class ONTParser extends ComputingNodesParser {

    public ONTParser(String file, TYPES type) {
		super(file, type);
	}

    @Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList datacenterList = xmlDoc.getElementsByTagName("ONTDevice");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			for (String element : List.of("isOrchestrator", "idleConsumption", "maxConsumption", "cores", "mips", "ram",
					"storage"))
				isElementPresent(datacenterElement, element);

			for (String element : List.of("cores", "mips", "ram", "storage"))
				assertDouble(datacenterElement, element, value -> (value >= 0), ">= 0. Check the file: " + file );

			assertDouble(datacenterElement, "idleConsumption", value -> (value >= 0),
					">= 0. Check the file " + file);
			double idleConsumption = Double
					.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
			assertDouble(datacenterElement, "maxConsumption", value -> (value >= idleConsumption),
					">= \"idleConsumption\". Check the file " + file);

			if (type == TYPES.ONT){																							        		
				SimulationParameters.numberOfONTs++;																				
			}

		}
		return true;
	}

}
