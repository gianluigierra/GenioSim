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
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.User;

public class NewApplicationFileParser extends XmlFileParser {

	public NewApplicationFileParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkAppFile();
	}

	protected boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String application = "\" application in \"";
		SimLog.println("%s - Checking applications file.",this.getClass().getSimpleName());
		SimulationParameters.applicationList = new ArrayList<>();
		Document doc;
		try (InputStream applicationFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(applicationFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttributePresent(appElement, "name");				//MODIFICA MIA, c'era scritto attribtue...

				for (String element : List.of("type", "latency", "container_size", "container_request_size", "shared", "request_size",
						"results_size", "task_length"))
					isElementPresent(appElement, element);

				// Latency-sensitivity in seconds.
				double latency = assertDouble(appElement, "latency", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file + "\" file");

				// The size of the container (bits).
				long containerSize = (long) (8000 * assertDouble(appElement, "container_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average request size (bits).
				long containerRequestSize = (long) (8000 * assertDouble(appElement, "container_request_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// If the container can be shared.
				boolean sharedContainer = Boolean.parseBoolean(appElement.getElementsByTagName("shared").item(0).getTextContent());

				// if the container is shared then we retrieve how many copies of it must be created
				int copies = 1;
				if(sharedContainer){
					copies = (int) assertDouble(appElement, "copies", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);
				}

				// Average request size (bits).
				// long requestSize = (long) (8000 * assertDouble(appElement, "request_size", value -> (value > 0),
				// 		condition + appElement.getAttribute("name") + application + file));

				//min and max request size (bits).
				long minRequestSize, maxRequestSize;
				String requestSizes = appElement.getElementsByTagName("request_size").item(0).getTextContent();
				if(requestSizes.contains(",")){
					String[] coppie = requestSizes.split(",");
					minRequestSize = 8000 * Long.parseLong(coppie[0]);
					maxRequestSize = 8000 * Long.parseLong(coppie[1]);
				}
				else{
					minRequestSize = 8000 * Long.parseLong(requestSizes);
					maxRequestSize = 8000 * Long.parseLong(requestSizes);
				}
				System.out.println("MinRequestSize = " + minRequestSize + ", MaxRequestSize = " + maxRequestSize);

				// Average downloaded results size (bits).
				long resultsSize = (long) (8000 * assertDouble(appElement, "results_size", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file));

				// Average task length (MI).
				double taskLength = assertDouble(appElement, "task_length", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The type of application.
				String type = appElement.getElementsByTagName("type").item(0).getTextContent();
				
				//The name of application
				String name = appElement.getAttribute("name");

				// Save applications parameters.
				SimulationParameters.applicationList.add(new Application(name, type, latency,
						containerSize, containerRequestSize, sharedContainer, copies, minRequestSize, maxRequestSize, resultsSize, taskLength));

				//creates the users for this application
				String usersType = appElement.getElementsByTagName("users_type").item(0).getTextContent();
				String[] coppie = usersType.split(";");
                for (String coppia : coppie) {
                    // Rimuove le parentesi quadre e divide in base alla virgola
                    String[] valori = coppia.replace("[", "").replace("]", "").split(",");
                    
                    // Parsa quantità e tipo
                    int quantita = Integer.parseInt(valori[0].trim());
                    int tipo = Integer.parseInt(valori[1].trim());
					for(User user : SimulationParameters.usersList){
						if(user.getType() == tipo){
							// Crea e aggiunge utenti del tipo specificato
							for (int j = 0; j < quantita; j++) {
								SimulationParameters.applicationList.get(SimulationParameters.applicationList.size()-1).addUser(new User(user));
							}
						}
					}
                }
			}

		} catch (Exception e) {
			SimLog.println("%s - Applications XML file cannot be parsed!",this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}

		SimLog.println("%s - Applications XML file successfully loaded!",this.getClass().getSimpleName());
		return true;
	}

}
