package cto.hmi.ner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class NerGetTime {
	public static List<String> TIME = new ArrayList<String>();

	public static String get() {
		String output = "";
		try {
			Document xmlDocument = DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.parse(new InputSource(new ByteArrayInputStream(
							NerProcessor.nerOutput.getBytes("utf-8"))));
			Node rootNode = xmlDocument.getFirstChild();
			if (rootNode.hasChildNodes()) {
				// Get each element child node
				NodeList elementsList = rootNode.getChildNodes();
				for (int i = 0; i < elementsList.getLength(); i++) {
					if (elementsList.item(i).hasChildNodes()) {
						// Get each tag child node to element node
						NodeList tagsList = elementsList.item(i)
								.getChildNodes();
						for (int i2 = 0; i2 < tagsList.getLength(); i2++) {
							Node tagNode = tagsList.item(i2);
							if (tagNode.getNodeName().matches("TIME"))
								TIME.add(tagNode.getTextContent());

						}
					}
				}
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < TIME.size(); i++) {
			output = output + "|" + TIME.get(i);
		}
		TIME.clear();
		return output;
	}
}
