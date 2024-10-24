package com.mechalikh.pureedgesim.scenariomanager;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;

//MODIFICA MIA, questa classe non esisteva. L'ho creata per effetuale il parsing degli ONT
public class SDNParser extends ComputingNodesParser {

    public SDNParser(String file, TYPES type) {
		super(file, type);
	}

    @Override
	protected boolean typeSpecificChecking(Document xmlDoc) {
		NodeList datacenterList = xmlDoc.getElementsByTagName("SDNDevice");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element sdnElement = (Element) datacenterNode;

			assertDouble(sdnElement, "idleConsumption", value -> (value >= 0),
					">= 0. Check the file " + file);
			double idleConsumption = Double
					.parseDouble(sdnElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
			assertDouble(sdnElement, "maxConsumption", value -> (value >= idleConsumption),
					">= \"idleConsumption\". Check the file " + file);
		}
		return true;
	}

}
