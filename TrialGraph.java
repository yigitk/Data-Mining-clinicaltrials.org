import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.lang.Integer;
import java.lang.Math;

public class TrialGraph {
    /* The path to the text file containing the information that will be used to build the graph: */
	private String dbFilePath;
    /* Variable to store the nodes of this graph: */
	private HashMap<TrialNode, TrialNode> nodes;
    /* Variable to store the edges of this graph: */
    private HashMap<TrialEdge, TrialEdge> edges;
    /* Strings used in the dbFilePath text file and elsewhere to identify nodes as a drug name, an adverse event name, or a condition name: */
	public static String drugIdentifierString = "d";
	public static String conditionIdentifierString = "c";
    public static String eventIdentifierString = "e";
	
	public TrialGraph(String dbFilePath) {
		this.dbFilePath = dbFilePath;
		this.nodes = new HashMap<TrialNode, TrialNode>();
        this.edges = new HashMap<TrialEdge, TrialEdge>();
	}
	
    /**
     * Go through the database text file line by line, extracting info 
     * from each line which will be converted into nodes and edges in 
     * the graph. The after this method is run, the graph will contain 
     * nodes representing drugs as well as nodes representing treated 
     * conditions and adverse events.
     */
	public void loadGraph() {
		try {
			String currentLine;
			BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(this.dbFilePath)));
            int i = 0;
            long startTime = System.currentTimeMillis();
			while ((currentLine = bufferedReader.readLine()) != null) {
                this.stringToNodes(currentLine);
                if (i % 1000 == 0) {
                    long stopTime = System.currentTimeMillis();
                    long elapsed = stopTime - startTime;
                    System.out.println("Have read " + i + " rows from the db file. Batch took " + elapsed + " milliseconds.");
                    startTime = System.currentTimeMillis();
                }
                i++;
			}
			bufferedReader.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
    /**
     * Iterate over this graph's list of nodes. If an EffectNode is 
     * encountered, all drug nodes connected to it are interconnected, 
     * and the EffectNode is removed from this graph's list of nodes.
     */
	private void pruneEffectNodes() {
        HashMap<TrialNode, TrialNode> newNodeList = new HashMap<TrialNode, TrialNode>();
        this.edges.clear();
        long startTime = System.currentTimeMillis();
        int numNodes = this.nodes.size();
        int i = 0;
        for (TrialNode n : this.nodes.values()) {
            if (i % 50 == 0) {
                long stopTime = System.currentTimeMillis();
                long elapsed = stopTime - startTime;
                System.out.println("Effect node pruning progress: " + ((int)((double)i / numNodes * 100)) + "%. Batch took " + elapsed + " milliseconds.");
                startTime = System.currentTimeMillis();
            }
            if (n instanceof EffectNode) {
                this.interConnect((EffectNode)n);
            }
            else {
                newNodeList.put(n, n);
            }
            i++;
        }
        this.nodes = newNodeList;
	}
	
    /**
     * Given an EffectNode, create an edge between every drug node 
     * connected to the EffectNode.
     */
	private void interConnect(EffectNode node) {
        TrialEdge e1;
        TrialEdge e2;
        for (int i = 0; i < node.getInvolvedEdges().size(); i++) {
            for (int j = i + 1; j < node.getInvolvedEdges().size(); j++) {
                e1 = node.getInvolvedEdges().get(i);
                e2 = node.getInvolvedEdges().get(j);
                if (!(e1.equals(e2))) {
                    TrialNode e1node;
                    TrialNode e2node;
                    if (!(e1.node1 instanceof EffectNode)) {
                        e1node = e1.node1;
                    }
                    else {
                        e1node = e1.node2;
                    }
                    if (!(e2.node1 instanceof EffectNode)) {
                        e2node = e2.node1;
                    }
                    else {
                        e2node = e2.node2;
                    }
                    TrialEdge newEdge = this.addEdge(e1node, e2node);
                    newEdge.weight += this.interConnectWeight(e1, e2);
                }
            }
        }
	}
	
    /**
     * Converts a line of text from the "database" text file into two 
     * connected nodes. A line of text has the following format:
     * 
     * Drug name~Effect name!Effect type!Phase
     * 
     * Effect type is either TrialGraph.conditionIdentifierString or 
     * TrialGraph.eventIdentifierString, depending on whether the effect 
     * was a treated condition or an adverse event in the original 
     * trial. The phase is the phase of the clinical trial.
     */
	private void stringToNodes(String line) {
        try {
            String[] firstSplit = line.split("~");
            TrialNode drugNode = new TrialNode(firstSplit[0]);
            String[] secondSplit = firstSplit[1].split("!");
            EffectNode effectNode = new EffectNode(secondSplit[0]);
            double weightChange = 0.0;
            if (secondSplit[1].equals(TrialGraph.conditionIdentifierString)) {
                weightChange = 1.0;
            }
            else {
                weightChange = -1.0;
            }
            int phase = Integer.parseInt(secondSplit[2]);
            weightChange *= phase;
            TrialNode newEffectNode = this.addNode(effectNode);
            TrialNode newTrialNode = this.addNode(drugNode);
            TrialEdge newEdge = this.addEdge(newTrialNode, newEffectNode);
            newEdge.weight += weightChange;
            ((EffectNode)newEffectNode).addInvolvedEdge(newEdge);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
			e.printStackTrace();
            System.out.println(line);
        }
	}
	
    /**
     * Add an edge to the graph, but only if the edge does not already 
     * exist.
     */
    private TrialEdge addEdge(TrialNode node1, TrialNode node2) {
        TrialEdge newEdge = new TrialEdge(node1, node2);
        boolean alreadyContains = this.edges.containsKey(newEdge);
        TrialEdge returnEdge = (alreadyContains ? this.edges.get(newEdge) : newEdge);
        if (!(alreadyContains)) {
            this.edges.put(newEdge, newEdge);
        }
        return returnEdge;
	}
	
    /**
     * Add a node to the graph, but only if the node doesn't already 
     * exist.
     */
	private TrialNode addNode(TrialNode node) {
        boolean alreadyContains = this.nodes.containsKey(node);
        TrialNode returnNode = (alreadyContains ? this.nodes.get(node) : node);
		if (!(alreadyContains)) {
			this.nodes.put(node, node);
		}
		return returnNode;
	}
    
    /**
     * Given an edge between some drug A and some effect X, and an edge 
     * between some drug B and effect X, this method determines the 
     * weight of the connection between A and B. The weight will be 
     * highest if A causes X and B treats X (indicating that X is an 
     * adverse event).
     */
    private double interConnectWeight(TrialEdge e1, TrialEdge e2) {
        if ((e1.weight > 0 && e2.weight < 0) || (e1.weight < 0 && e2.weight > 0)) {
            return Math.abs(e1.weight) + Math.abs(e2.weight);
        }
        else {
            return (e1.weight + e2.weight) / 2;
        }
    }
    
    public HashMap<TrialEdge, TrialEdge> getEdges() {
        return this.edges;
    }
    
    public int getNumberOfNodes() {
		return this.nodes.size();
	}
    
    public int getNumberOfEdges() {
        return this.edges.size();
    }
	
	public void printGraphToFile(String directory) {
		try {
			File targetFile = new File(directory + "/graph.txt");
			targetFile.createNewFile();
			FileWriter fileWriter = new FileWriter(targetFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (TrialEdge e : this.edges.values()) {
                bufferedWriter.write(e.toString() + "\n");
                bufferedWriter.flush();
            }
            bufferedWriter.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
    
    /**
     * A class representing a node in the graph. Could be a node 
     * representing a drug, a node representing a treated condition, or 
     * a node representing an adverse event.
     */
    public static class TrialNode implements Comparable<TrialNode> {
		private String contents;
		
		public TrialNode(String contents) {
			this.contents = contents;
		}
		
		public String getContents() {
			return this.contents;
		}
        
        @Override
        public int compareTo(TrialNode node) {
			return this.contents.compareToIgnoreCase(node.getContents());
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof TrialNode) {
				int comparison = this.compareTo((TrialNode)o);
				return comparison == 0;
			}
			else {
				return false;
			}
		}
        
        @Override
        public int hashCode() {
            return this.contents.hashCode();
        }
	}
    
    /**
     * A class for adverse event and treated condition nodes in the 
     * graph. Primary difference from TrialNode is that an EffectNode 
     * keeps track of a subset of the graph's edges. Specifically, the 
     * effect node dynamically keeps track of all edges connected to it. 
     * This allows for better efficiency when pruning EffectNodes from 
     * the graph.
     */
    public static class EffectNode extends TrialNode {
        private ArrayList<TrialEdge> edges;
        
        public EffectNode(String contents) {
            super(contents);
            this.edges = new ArrayList<TrialEdge>(10);
        }
        
        public boolean addInvolvedEdge(TrialEdge edge) {
            if (this.edges.contains(edge)) {
                return false;
            }
            else {
                this.edges.add(edge);
                return true;
            }
        }
        
        public ArrayList<TrialEdge> getInvolvedEdges() {
            return this.edges;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof EffectNode) {
				int comparison = this.compareTo((EffectNode)o);
				return comparison == 0;
			}
			else {
				return false;
			}
        }
    }
    
    /**
     * A class for edges between TrialNodes.
     */
    public static class TrialEdge implements Comparable<TrialEdge> {
        public double weight;
        public TrialNode node1;
        public TrialNode node2;
        public int occurrences;
        
        public TrialEdge(TrialNode node1, TrialNode node2) {
            this.weight = 0.0;
            this.occurrences = 1;
            int comparison = node1.compareTo(node2);
            if (comparison <= 0) {
                this.node1 = node1;
                this.node2 = node2;
            }
            else {
                this.node1 = node2;
                this.node2 = node1;
            }
        }
        
        @Override
        public int compareTo(TrialEdge edge) {
            int node1comparison = this.node1.getContents().compareToIgnoreCase(edge.node1.getContents());
            if (node1comparison == 0) {
                return this.node2.getContents().compareToIgnoreCase(edge.node2.getContents());
            }
            else {
                return node1comparison;
            }
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof TrialEdge) {
                TrialEdge otherEdge = (TrialEdge)o;
                boolean b1 = this.node1.equals(otherEdge.node1) && this.node2.equals(otherEdge.node2);
                boolean b2 = this.node1.equals(otherEdge.node2) && this.node2.equals(otherEdge.node1);
                return b1 || b2;
            }
            else {
                return false;
            }
        }
        
        @Override
        public String toString() {
            return this.node1.getContents() + "--- (" + this.weight + ") ---" + this.node2.getContents();
        }
        
        @Override
        public int hashCode() {
            return this.node1.getContents().hashCode();
        }
    }
    
    /**
     * A Comparator just for use in the main method, when the graph's 
     * edges are sorted for the purpose of retrieving the top 50 highest 
     * weighted edges and the top 50 lowest weighted edges.
     */
    public static class EdgeWeightComparator implements Comparator<TrialEdge> {
        /**
         * TrialEdges are sorted by weight.
         */
        public int compare(TrialEdge e1, TrialEdge e2) {
            if (e1.weight < e2.weight) {
                return -1;
            }
            else if (e1.weight > e2.weight) {
                return 1;
            }
            else {
                return 0;
            }
        }
        
        public boolean equals(Object o) {
            return false;
        }
    }
	
	public static void main(String[] args) {
		TrialGraph g = new TrialGraph("/home/andy/programs/java/EECS 435 project/indirectDB.txt");
		g.loadGraph();
        int numNodesBeforePrune = g.getNumberOfNodes();
        int numEdgesBeforePrune = g.getNumberOfEdges();
        g.pruneEffectNodes();
        int numNodesAfterPrune = g.getNumberOfNodes();
        int numEdgesAfterPrune = g.getNumberOfEdges();
        System.out.println("Before pruning effect nodes: " + numNodesBeforePrune + " nodes and " + numEdgesBeforePrune + " edges.");
        System.out.println("After pruning effect nodes: " + numNodesAfterPrune + " nodes and " + numEdgesAfterPrune + " edges.");
        g.printGraphToFile("/home/andy/programs/java/EECS 435 project");
        
        ArrayList<TrialEdge> sortedList = new ArrayList<TrialEdge>(g.getEdges().size());
        for (TrialGraph.TrialEdge e : g.getEdges().values()) {
            sortedList.add(e);
        }
        Collections.sort(sortedList, new EdgeWeightComparator());
        System.out.println("\nMost negative connections:");
        for (int i = 0; i < 50; i++) {
            System.out.println(sortedList.get(i));
        }
        System.out.println("\nMost positive connections:");
        for (int i = sortedList.size() - 1; i > sortedList.size() - 50; i--) {
            System.out.println(sortedList.get(i));
        }
	}
}
