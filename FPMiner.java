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

/**
 * A quick class I wrote to print all of the frequent 2-patterns from 
 * the 19,000 clinical trials we mined.
 */
public class FPMiner {
    public int minSupport = 50;
    public String directory;
    
    public FPMiner(String directory) {
        this.directory = directory;
    }
    
    public void mineFrequentPatterns() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(directory + "/FPDB.txt")));
            HashSet<String> set = this.getDistinctDrugs();
            ArrayList<String> frequentOnePatterns = this.getFrequentOnePatterns(set);
            ArrayList<String> frequentTwoPatterns = this.getFrequentTwoPatterns(frequentOnePatterns);
            for (String s : frequentTwoPatterns) {
                System.out.println(s);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
			e.printStackTrace();
        }
    }
    
    private HashSet<String> getDistinctDrugs() {
        try {
            HashSet<String> set = new HashSet<String>();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(directory + "/FPDB.txt")));
            String currentLine;
            String[] strings;
            while ((currentLine = bufferedReader.readLine()) != null) {
                strings = currentLine.split("!");
                for (int i = 0; i < strings.length; i++) {
                    set.add(strings[i]);
                }
            }
            return set;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
			e.printStackTrace();
        }
        return null;
    }
    
    private ArrayList<String> getFrequentOnePatterns(HashSet<String> set) {
        try {
            ArrayList<String> patterns = new ArrayList<String>();
            for (String s : set) {
                int support = 0;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(directory + "/FPDB.txt")));
                String currentLine;
                while ((currentLine = bufferedReader.readLine()) != null) {
                    String[] strings = currentLine.split("!");
                    for (int i = 0; i < strings.length; i++) {
                        if (strings[i].compareToIgnoreCase(s) == 0) {
                            support++;
                        }
                    }
                }
                if (support >= minSupport) {
                    patterns.add(s);
                }
            }
            return patterns;
        }
        catch (Exception e) {
            
        }
        return null;
    }
    
    private ArrayList<String> getFrequentTwoPatterns(ArrayList<String> frequentOnePatterns) {
        ArrayList<String> patterns = new ArrayList<String>();
        try {
            for (int i = 0; i < frequentOnePatterns.size(); i++) {
                for (int j = i; j < frequentOnePatterns.size(); j++) {
                    int support = 0;
                    String s1 = frequentOnePatterns.get(i);
                    String s2 = frequentOnePatterns.get(j);
                    if (!(s1.equals(s2))) {
                        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(directory + "/FPDB.txt")));
                        String currentLine;
                        while ((currentLine = bufferedReader.readLine()) != null) {
                            HashSet<String> set = new HashSet<String>();
                            String[] strings = currentLine.split("!");
                            for (int k = 0; k < strings.length; k++) {
                                set.add(strings[k]);
                            }
                            if (set.contains(s1) && set.contains(s2)) {
                                support++;
                            }
                        }
                        if (support >= minSupport) {
                            patterns.add(s1 + ", " + s2);
                        }
                    }
                }
            }
            return patterns;
        }
        catch (Exception e) {
            
        }
        return null;
    }
    
    public static void main(String[] args) {
        FPMiner miner = new FPMiner("/home/andy/programs/java/EECS 435 project/");
        miner.mineFrequentPatterns();
    }
}
