/**
 * Useful fields:
 * - <intervention_type> - should be "Drug"
 * - <intevention_name> - the name/description of the treatment
 * - <group group_id="..."> <title>TREATMENT RECEIVED</title>
 * - <event> - adverse event, with number of subjects affected as well as number of subjects vulnerable
 * - <sub_title> - name of an adverse event
 * - <counts> - attributes contain information that can be used to calculate percentage of people who experienced adverse event
 * --- group_id, subjects_affected, subjects_at_risk - all useful attributes of <counts>
 * 
 * Possibly useful fields:
 * - <primary_outcome>, <secondary_outcome>, <condition> - the condition treated by the drug?
 * - <condition_browse> <mesh_term> - Standardized names of the conditions treated in a study
 * - <intervention_browse> <mesh_term> - Standardized names of the interventions in a study
 */

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;

/**
 * A class for reading clinical trial XML files and retrieving info from 
 * them that is relevant to the graph.
 */
public class TrialReader extends XMLReader {
    /**
     * Generate a "database" (a text file where each line of text 
     * represents a tuple in the "database") that will be read to 
     * construct the drug graph. This method loops through every 
     * clinical trial file in a specified directory, and extracts info 
     * that will be used for generating and weighting the graph.
     */
	public void generateIndirectDatabase(String dataDirectory, String targetDirectory) {
		try {
			/* Get the names of all files in the directory: */
			File folder = new File(dataDirectory);
			File[] trialFiles = folder.listFiles();
			/* Set up a new text file, and a BufferedWriter to write to it: */
			File targetFile = new File(targetDirectory + "/indirectDB.txt");
			targetFile.createNewFile();
			FileWriter fileWriter = new FileWriter(targetFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			/* Extract info from each file in the directory one at a time: */
			String fileName;
			ArrayList<String> dbStringList;
			for (int i = 0; i < trialFiles.length; i += 1) {
                /* Progress tracking: */
                if (i % 100 == 0) {
                    System.out.println("Graph file progress: " + ((int)((double)i / trialFiles.length * 100)) + "%");
                }
				fileName = dataDirectory + "/" + trialFiles[i].getName();
				dbStringList = this.generateDatabaseStrings(fileName);
				for (String s : dbStringList) {
					bufferedWriter.write(s + "\n");
					bufferedWriter.flush();
				}
			}
			bufferedWriter.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
    /**
     * Extract information from a specified clinical trial that will be 
     * used for building the graph.
     */
	private ArrayList<String> generateDatabaseStrings(String fileName) {
		ArrayList<String> returnList = new ArrayList<String>();
		StringBuilder builder;
		ArrayList<String> interventions = this.getMeshInterventionNames(fileName);
		ArrayList<String> events = this.getEventTitles(fileName);
		ArrayList<String> conditions = this.getMeshTreatedConditions(fileName);
        String phase = this.getPhase(fileName);
		for (String i : interventions) {
			for (String e : events) {
				builder = new StringBuilder();
				builder.append(i);
				builder.append("~");
				builder.append(e);
                builder.append("!");
                builder.append(TrialGraph.eventIdentifierString);
                builder.append("!");
                builder.append(phase);
				returnList.add(builder.toString());
			}
			for (String c : conditions) {
				builder = new StringBuilder();
				builder.append(i);
				builder.append("~");
				builder.append(c);
                builder.append("!");
                builder.append(TrialGraph.conditionIdentifierString);
                builder.append("!");
                builder.append(phase);
				returnList.add(builder.toString());
			}
		}
		return returnList;
	}
    
    /**
     * Generate a "database" (a text file where each line of text 
     * represents a tuple in the "database") where each row contains the 
     * names of the drugs used in a single clinical trial. Frequent 
     * 2-patterns will be mined from the database.
     */
    public void generateFPDatabase(String dataDirectory, String targetDirectory) {
        try {
			/* Get the names of all files in the directory: */
			File folder = new File(dataDirectory);
			File[] trialFiles = folder.listFiles();
			/* Set up a new text file, and a BufferedWriter to write to it: */
			File targetFile = new File(targetDirectory + "/FPDB.txt");
			targetFile.createNewFile();
			FileWriter fileWriter = new FileWriter(targetFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			/* Extract info from each file in the directory one at a time: */
			String fileName;
			String dbString;
			for (int i = 0; i < trialFiles.length; i += 1) {
                /* Progress tracking: */
                if (i % 100 == 0) {
                    System.out.println("FP file progress: " + ((int)((double)i / trialFiles.length * 100)) + "%");
                }
				fileName = dataDirectory + "/" + trialFiles[i].getName();
				dbString = this.generateFPString(fileName);
                if (dbString.length() > 0) {
                    bufferedWriter.write(dbString + "\n");
                    bufferedWriter.flush();
                }
			}
			bufferedWriter.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
    }
    
    /**
     * Extract information from a specified clinical trial that will be 
     * stored in the frequent pattern database.
     */
    private String generateFPString(String fileName) {
        ArrayList<String> returnList = new ArrayList<String>();
		StringBuilder builder = new StringBuilder();
		ArrayList<String> interventions = this.getMeshInterventionNames(fileName);
        for (String i : interventions) {
            builder.append(i + "!");
        }
        return builder.toString();
    }
    
    /**
     * Extract the phase number from a clinical trial. It will be 1, 2, 
     * 3, or 4. If the phase is poorly formatted or otherwise invalid, 
     * the phase will be stored as zero.
     */
    private String getPhase(String fileName) {
        String phaseString = this.getContent(this.getFirstMatchingElement(this.getRootElement(fileName), "phase"));
        if (phaseString.length() != 7) {
            return "0";
        }
        else {
            return phaseString.substring(6);
        }
    }
    
    /**
     * Extract the list of drug names from a clinical trial.
     */
	private ArrayList<String> getInterventionNames(String fileName) {
		ArrayList<String> list = new ArrayList<String>(5);
		NodeList interventions = this.getElements(this.getRootElement(fileName), "intervention");
		for (int i = 0; i < interventions.getLength(); i++) {
            Element currentNode = (Element)interventions.item(i);
            if (this.getContent(this.getFirstMatchingElement(currentNode, "intervention_type")).compareToIgnoreCase("Drug") == 0) {
                list.add(this.getContent(this.getFirstMatchingElement(currentNode, "intervention_name")));
            }
		}
		return list;
	}
    
    /**
     * Extract the list of Medical Subject Heading names of all of the 
     * drugs used in a clinical trial (MeSH terms are more standardized 
     * than the information extracted by getInterventionNames() ).
     */
    private ArrayList<String> getMeshInterventionNames(String fileName) {
        ArrayList<String> list = new ArrayList<String>(5);
		NodeList interventionBrowse = this.getElements(this.getRootElement(fileName), "intervention_browse");
        if (interventionBrowse.getLength() == 0) {
            return list;
        }
        NodeList interventions = this.getElements((Element)interventionBrowse.item(0), "mesh_term");
		for (int i = 0; i < interventions.getLength(); i++) {
            list.add(this.getContent(interventions.item(i)));
		}
		return list;
    }
	
    /**
     * Extract the list of adverse events from a clinical trial.
     */
	private ArrayList<String> getEventTitles(String fileName) {
		ArrayList<String> list = new ArrayList<String>(20);
		NodeList events = this.getElements(this.getRootElement(fileName), "event");
        if (events.getLength() == 0) {
            return list;
        }
		for (int i = 0; i < events.getLength(); i++) {
			Element event = (Element)events.item(i);
            String title = this.getContent(this.getFirstMatchingElement(event, "sub_title"));
            if (!(title == null) && !(title.equals("Total, other adverse events") || title.equals("Total, serious adverse events"))) {
                list.add(title);
            }
		}
		return list;
	}
	
    /**
     * Extract the list of treated conditions from a clinical trial.
     */
	private ArrayList<String> getTreatedConditions(String fileName) {
		ArrayList<String> list = new ArrayList<String>(5);
		NodeList conditions = this.getElements(this.getRootElement(fileName), "condition");
		for (int i = 0; i < conditions.getLength(); i++) {
			list.add(this.getContent(conditions.item(i)));
		}
		return list;
	}
    
    /**
     * Extract the list of Medical Subject Heading names of all of the 
     * treated conditions in a clinical trial (MeSH terms are more 
     * standardized than the information extracted by 
     * getTreatedConditions() ).
     */
    private ArrayList<String> getMeshTreatedConditions(String fileName) {
        ArrayList<String> list = new ArrayList<String>(5);
		NodeList conditionBrowse = this.getElements(this.getRootElement(fileName), "condition_browse");
        if (conditionBrowse.getLength() == 0) {
            return list;
        }
        NodeList conditions = this.getElements((Element)conditionBrowse.item(0), "mesh_term");
		for (int i = 0; i < conditions.getLength(); i++) {
			list.add(this.getContent(conditions.item(i)));
		}
		return list;
    }
    
    /**
     * Generate the "databases" (text files) containing all information 
     * that will be used for generating and validating the graph.
     */
    public static void main(String[] args) {
		String dataDirectory = "/home/andy/programs/java/EECS 435 project/trials data";
		String targetDirectory = "/home/andy/programs/java/EECS 435 project";
		TrialReader reader = new TrialReader();
		reader.generateIndirectDatabase(dataDirectory, targetDirectory);
        reader.generateFPDatabase(dataDirectory, targetDirectory);
	}
}
