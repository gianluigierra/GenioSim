package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.User;

public class UsersFileParser  extends XmlFileParser {

	public UsersFileParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkAppFile();
	}

	protected boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String user = "\" user in \"";
		SimLog.println("%s - Checking users file.",this.getClass().getSimpleName());
		SimulationParameters.usersList = new ArrayList<>();
		Document doc;
		try (InputStream userFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(userFile);
			doc.getDocumentElement().normalize();

			NodeList userList = doc.getElementsByTagName("user");
			for (int i = 0; i < userList.getLength(); i++) {
				Node userNode = userList.item(i);

				Element userElement = (Element) userNode;
				isAttributePresent(userElement, "type");				

				for (String element : List.of("access_pattern", "rate", "start", "duration", "interval"))
					isElementPresent(userElement, element);
                    
				//The type of User			
				String type = userElement.getAttribute("type");

				// The access pattern of the user.
				String accessPattern = userElement.getElementsByTagName("access_pattern").item(0).getTextContent();

				// The generation rate (tasks per minute)
				int rate = (int) assertDouble(userElement, "rate", value -> (value > 0),
						condition + userElement.getAttribute("type") + user + file);

				// The start (time in minutes)
				int start = (int) assertDouble(userElement, "start", value -> (value >= 0),
						condition + userElement.getAttribute("type") + user + file);

				// The duration (time in minutes)
				double duration;
				String durationString = userElement.getElementsByTagName("duration").item(0).getTextContent();
				if(durationString.equals("infinity"))
					duration = SimulationParameters.simulationDuration/60;
				else duration = Double.valueOf(durationString);

				// The interval (time in minutes)
				int interval;
				if(durationString.equals("infinity")) interval = (int) SimulationParameters.simulationDuration/60;
				else interval = (int) assertDouble(userElement, "interval", value -> (value >= 0),
						condition + userElement.getAttribute("type") + user + file);

				// Save user's parameters.
				SimulationParameters.usersList.add(new User(Integer.valueOf(type), accessPattern, rate, start, duration, interval));

			}

		} catch (Exception e) {
			SimLog.println("%s - Users XML file cannot be parsed!",this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}

		SimLog.println("%s - Users XML file successfully loaded!",this.getClass().getSimpleName());
		return true;
	}

}