import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

/*
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;;
*/

// You should call this code as follows:
//
//   java WebSearch directoryName searchStrategyName
//   (or jview, in J++)
//
//   where <directoryName> is the name of corresponding intranet
//   and <searchStrategyName> is one of {breadth, depth, best, beam}.

// The PARTIAL code below contains code for fetching and parsing
// the simple web pages we're using, as well as the fragments of
// a solution.  BE SURE TO READ ALL THE COMMENTS.

// Feel free to alter or discard whatever code you wish;
// the only requirement is that your main class be called WebSearch
// and that it accept the two arguments described above
// (if you wish you can add additional OPTIONAL arguments, but they
// should default to the values "hardwired" in below).

public class WebAdventure {
	static LinkedList<SearchNodeADV> OPEN; // Feel free to choose your own data structures for searching,
	static HashSet<String> CLOSED;      // and be sure to read documentation about them.
	static Stack<SearchNodeADV> VISITED;
	static LinkedList<String> PATH; 

	static final boolean DEBUGGING = false; // When set, report what's happening.
	// WARNING: lots of info is printed.

	static int beamWidth = 20; // If searchStrategy = "beam",
	// limit the size of OPEN to this value.
	// The setSize() method in the Vector
	// class can be used to accomplish this.

	static String START_NODE     = null;
	
	// Keys associated with the goal pattern
	static String[] keys = {"Computer", "Computer Science", "Engineering", "Software" };

	// A web page is a goal node if it includes 
	// the following string.
	static final String GOAL_PATTERN   ="graduate";

	public static void main(String args[]) throws IOException
	{ 
		if (args.length != 2)
		{
			System.out.println("You must provide the directoryName and searchStrategyName.  Please try again.");
		}
		else
		{
			URL url = new URL(args[0]);
			
			START_NODE = url.toString();
			String searchStrategyName = args[1]; // Read the search strategy to use.

			if (searchStrategyName.equalsIgnoreCase("breadth") ||
					searchStrategyName.equalsIgnoreCase("depth")   ||
					searchStrategyName.equalsIgnoreCase("best")    ||
					searchStrategyName.equalsIgnoreCase("beam"))
			{
				performSearch(START_NODE, url, searchStrategyName);
			}
			else
			{
				System.out.println("The valid search strategies are:");
				System.out.println("  BREADTH DEPTH BEST BEAM");
				
			}
		}

		Utilities.waitHere("Press ENTER to exit.");
	}
	
	static void performSearch(String startNode, URL url, String searchStrategy) throws IOException
	{
		boolean solution_found = false;
		int nodesVisited = 0;
		PATH = new LinkedList<String>();
		OPEN   = new LinkedList<SearchNodeADV>();
		CLOSED = new HashSet<String>();
		VISITED = new Stack<SearchNodeADV>();
		
		OPEN.add(new SearchNodeADV(startNode));
		
		while (!OPEN.isEmpty())
		{
			SearchNodeADV currentNode = OPEN.pop();
			String currentURL = currentNode.getNodeName();

			nodesVisited++;

			// Go and fetch the contents of this file.
			String contents = UtilitiesADV.getFileContents(url);
			
			if (isaGoalNode(contents))
			{
				// Report the solution path found
				// (You might also wish to write a method that
				// counts the solution-path's length, and then print that
				// number here.)
				solution_found = true;
				System.out.print("\n	");
				int length = currentNode.reportSolutionPath();
				System.out.println("Path Size: " + length);
				break;
			}

			// Remember this node was visited.
			CLOSED.add(currentURL);
			VISITED.push(currentNode);
			addNewChildrenToOPEN(currentNode, url, contents, searchStrategy);
			
			// Provide a status report.
			if (DEBUGGING) System.out.println("Nodes visited = " + nodesVisited
					+ " |OPEN| = " + OPEN.size());
		}	
		
		if(!solution_found) { System.out.println("NO SOLUTION\n"); }
		
		System.out.println();
		System.out.println("	Visited " + nodesVisited + " nodes, starting @" + startNode +
				", using: " + searchStrategy + " search.");
	}


	// This method reads the page's contents and
	// collects the 'children' nodes (ie, the hyperlinks on this page).
	// The parent node is also passed in so that 'backpointers' can be
	// created (in order to later extract solution paths).
	static void addNewChildrenToOPEN(SearchNodeADV parent, URL url, String contents, String searchStrategy) throws IOException
	{
		// StringTokenizer's are a nice class built into Java.
		// Be sure to read about them in some Java documentation.
		// They are useful when one wants to break up a string into words (tokens).
		
		Document doc;
		doc = Jsoup.connect(url.toString()).get();
		Elements hyperlinks = doc.getElementsByTag("a");
		
		
		for(Element link : hyperlinks){
			String l = link.attr("href");
			if(l.length() > 0) {
				if(l.length() < 4){
					l = doc.baseUri()+l.substring(1);
				} else if(!l.substring(0, 4).equals("http")){
					l = doc.baseUri()+l.substring(1);
				}
				System.out.println(l);
				SearchNodeADV new_node = new SearchNodeADV(l);
				new_node.setHvalue(contains_keys(l));
				parent.addNeighbor(l);
				
				if(searchStrategy.equalsIgnoreCase("breadth") || searchStrategy.equalsIgnoreCase("best") || searchStrategy.equalsIgnoreCase("beam")){
					OPEN.add(new_node);
				} 
				else if(searchStrategy.equalsIgnoreCase("depth")){
					OPEN.push(new_node);
				}
			}
		}
		if(searchStrategy.equalsIgnoreCase("best") || searchStrategy.equalsIgnoreCase("beam")){
			sort(OPEN);
			if(searchStrategy.equalsIgnoreCase("beam")){
				while(OPEN.size() > beamWidth){
					OPEN.removeLast();
				}
			}
		}
		
		
	}
	
	static int contains_keys(String hyperlink){
		int hVal = 0;
		for(int i = 0; i < WebAdventure.keys.length; i++){
			if(hyperlink.contains(WebAdventure.keys[i])){
				hVal++;
			}
		}
		return hVal;
	}
	
	/**
	 * A simple sort algorithm to sort in decending order.
	 * @param list
	 */
	static void sort(LinkedList<SearchNodeADV> list){
		for(int i = 0; i < list.size(); i++){
			for(int j = 0; j < list.size(); j++){
				if(list.get(i).getHvalue() > list.get(j).getHvalue()){
					SearchNodeADV temp = list.get(i);
					list.set(i, list.get(j));
					list.set(j, temp);
				}
			}
		}
	}

	// A GOAL is a page that contains the goalPattern set above.
	static boolean isaGoalNode(String contents)
	{
		return (contents != null && contents.indexOf(GOAL_PATTERN) >= 0);
	}

	// Is this hyperlink already in the OPEN list?
	// This isn't a very efficient way to do a lookup,
	// but its fast enough for this homework.
	// Also, this for-loop structure can be
	// be adapted for use when inserting nodes into OPEN
	// according to their heuristic score.
	static boolean alreadyInOpen(String hyperlink)
	{
		int length = OPEN.size();

		for(int i = 0; i < length; i++)
		{
			SearchNodeADV node = OPEN.get(i);
			String oldHyperlink = node.getNodeName();

			if (hyperlink.equalsIgnoreCase(oldHyperlink)) return true;  // Found it.
		}

		return false;  // Not in OPEN.    
	}

	// You can use this to remove the first element from OPEN.
	static SearchNodeADV pop(LinkedList<SearchNodeADV> list)
	{
		SearchNodeADV result = list.removeFirst();




		return result;
	}
}

/////////////////////////////////////////////////////////////////////////////////

// You'll need to design a Search node data structure.

// Note that the above code assumes there is a method called getHvalue()
// that returns (as a double) the heuristic value associated with a search node,
// a method called getNodeName() that returns (as a String)
// the name of the file (eg, "page7.html") associated with this node, and
// a (void) method called reportSolutionPath() that prints the path
// from the start node to the current node represented by the SearchNode instance.
class SearchNodeADV
{
	final String nodeName;
	int Hval = 0;
	LinkedList<String> neighbors;
	
	public SearchNodeADV(String name) {
		nodeName = name;
		neighbors = new LinkedList<String>();
	}
	
	/**
	 * Modified to print the solution path and return the length of the path
	 */
	public int reportSolutionPath() {
		SearchNodeADV cur = this;
		WebAdventure.PATH.push(cur.nodeName);
		while(!WebAdventure.VISITED.isEmpty()){
			SearchNodeADV next = WebAdventure.VISITED.pop();
			if(next.has_neighbor(cur)){
				WebAdventure.PATH.push(next.getNodeName());
				cur = next;
			}
		}
		for(String node : WebAdventure.PATH){
			System.out.print(node + " -> ");
		}
		//return the length - 1 so account for path length not number of nodes 
		return WebAdventure.PATH.size() - 1;
	}
	
	public void addNeighbor(String str){
		neighbors.add(str);
	}
	
	public boolean has_neighbor(SearchNodeADV check){
		for(String neighbor : this.neighbors){
			if(neighbor.equals(check.getNodeName())){
				return true;
			}
		} return false;
	}
	
	public void printNeighbors(){
		for(String neighbor : neighbors){
			System.out.print(neighbor + " | ");
		}
	}

	public String getNodeName() {
		return nodeName;
	} 
	
	public int getHvalue() {
		return Hval;
	}
	
	public void setHvalue(int val){
		this.Hval = val;
	}
}

/////////////////////////////////////////////////////////////////////////////////

// Some 'helper' functions follow.  You needn't understand their internal details.
// Feel free to move this to a separate Java file if you wish.
class UtilitiesADV
{
	// In J++, the console window can close up before you read it,
	// so this method can be used to wait until you're ready to proceed.
	public static void waitHere(String msg)
	{
		System.out.println("");
		System.out.println(msg);
		try { System.in.read(); } catch(Exception e) {} // Ignore any errors while reading.
	}

	// This method will read the contents of a file, returning it
	// as a string.  (Don't worry if you don't understand how it works.)
	public static synchronized String getFileContents(URL url)
	{
		String results = "";
		try {
			BufferedReader buff = new BufferedReader(new InputStreamReader(url.openStream()));
			String str;
			while((str = buff.readLine()) != null){
				results += buff.readLine().toString();
			}
			buff.close();
			
		} catch (Exception e) {
			System.out.println("Exception");
		}

		return results;
	}
}


