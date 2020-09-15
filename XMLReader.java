import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;

/**
 * A class for retrieving pieces of information from an XML file.
 */
public class XMLReader {
	/**
	 * Return an Element object that provides access to an entire XML 
	 * file.
	 */
	public Element getRootElement(String xmlFileName) {
        Document doc;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(xmlFileName);
            Element rootElement = doc.getDocumentElement();
            return rootElement;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    /**
     * Return a list of elements (nodes) matching a specified tag 
     * (element name). Only returns elements within a specified parent 
     * element.
     */
    public NodeList getElements(Element rootElement, String elementName) {
		NodeList nodeList = rootElement.getElementsByTagName(elementName);
		return nodeList;
	}
	
	/**
	 * A method for retrieving elements corresponding to tag names that 
	 * only occur once within a specific parent element.
	 */
	public Node getFirstMatchingElement(Element rootElement, String elementName) {
		NodeList elements = this.getElements(rootElement, elementName);
		return elements.item(0);
	}
	
	/**
	 * Return the text surrounded by the tags represented by a Node 
	 * object.
	 */
	public String getContent(Node node) {
        if (node == null) {
            return null;
        }
        else {
            return node.getTextContent();
        }
	}
	
	/**
	 * Return a string containing the text surrounded by the tags 
	 * represented by a Node object, followed by a list of the attribute 
	 * names and their values.
	 */
	public String getContentWithAttributes(Node node) {
		StringBuilder builder = new StringBuilder();
		String textContent = this.getContent(node);
		builder.append(textContent + " ");
		NamedNodeMap attributes = node.getAttributes();
		if (attributes != null && attributes.getLength() > 0) {
			for (int j = 0; j < attributes.getLength(); j++) {
				Node attribute = attributes.item(j);
				builder.append(attribute.getNodeName() + " = \"" + attribute.getNodeValue() + "\" ");
			}
		}
		return builder.toString();
	}
	
	/**
	 * Print the text contained within an element, as well as all the 
	 * attribute values of the element.
	 */
	public void printElement(Node node) {
		System.out.println(node.getNodeName() + ": " + node.getTextContent());
		NamedNodeMap attributes = node.getAttributes();
		if (attributes != null && attributes.getLength() > 0) {
			for (int j = 0; j < attributes.getLength(); j++) {
				Node attribute = attributes.item(j);
				System.out.println(attribute.getNodeName() + " = \"" + attribute.getNodeValue() + "\"");
			}
		}
	}
	
	/**
     * Print a list of elements.
     */
    public void printElements(NodeList nodeList) {
		if (nodeList != null && nodeList.getLength() > 0 ) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				this.printElement(node);
				System.out.println("\n");
			}
		}
    }
}

