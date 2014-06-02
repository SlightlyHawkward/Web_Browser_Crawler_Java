import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;


public class HelloWorld extends HttpServlet 
{ 
	private static final long serialVersionUID = 1L;
	public static Statement stat; //new
	public static Statement altstat;
	public static Connection connection;
	public static int foundURLs;
	public static PrintWriter out;
	public String localSQL = "jdbc:mysql://localhost:3306/webdata";
	public String user = "root";
	public String pw = "password";
	public static String drivers = "com.mysql.jdbc.Driver";
	
	static{
		//System.setProperty("jdbc.drivers", drivers);		
	}
	
	protected void doGet(HttpServletRequest request, 
      HttpServletResponse response) throws ServletException, IOException 
      {
		// reading the user input
		String searchString= request.getParameter("color");    
		String searchArray[] = searchString.toLowerCase().split(" ");
		out = response.getWriter();
		String docType =
				"<!doctype html public \"-//w3c//dtd html 4.01 transitional//en\"" +
						"\"http://www.w3.org/TR/html4/loose.dtd\">\n";
		out.println (docType + 
    		"<html> \n" +
    		"<head> \n" +
    		"<title> Search Results </title> \n" +
    		"</head> \n" +
    		"<body> \n" +
    		"<font size=\"12px\" color=\"red\">" +
    		"Results for: " + searchString + " <br> " +
    		"</font> \n" 
					);  
		// Establish connection to the SQL server
		try {
	        Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(localSQL, user, pw);
			//out.println("Connection Success <br>");
		} catch (SQLException e) {
			e.printStackTrace();
			out.println("Connection Failed <br>");
			out.println("SQLState: " + ((SQLException)e).getSQLState() + "<br>");
			out.println("Error Code: " + ((SQLException)e).getErrorCode() + "<br>");
			out.println("Message: " + (e).getMessage() + "<br>");
			Throwable t = e.getCause();
			while(t != null){
				out.println("Cause: " + t);
				t = t.getCause();
			}
		
		} catch(Exception e){
	         //Handle errors for Class.forName
	         e.printStackTrace();
	    }
		
		try{
			stat = connection.createStatement();
			altstat = connection.createStatement();
		} catch (SQLException e) {
	         e.printStackTrace();
	         out.println("Statement creation messed up bro <br>");
		}
		
		String wordQuery = " ";
		
		HashMap<Integer, Integer> foundIDs = new HashMap<Integer, Integer>();
		ArrayList<Integer> commonIDsArray = new ArrayList<Integer>();
		//int id = 0;
		boolean populated = false;
		int numberfound = 0;
		//explore the hashmap map of words ids
		try{
			for(String searchWord : searchArray){
				//Query database
				wordQuery = "SELECT urlid FROM words WHERE word IN('" + searchWord + "')";
				//out.println("Query: " + wordQuery + "<br>");	
				ResultSet wordResults = stat.executeQuery(wordQuery);
	        	if(populated == false){
	    			while(wordResults.next()) // populate the initial hashmap
	            	{   
	    				Integer found = Integer.valueOf(wordResults.getInt("urlid"));
	    				//out.println(found + ", ");
	    				numberfound++;
	    				foundIDs.put(found, found);
	            	}
    				//out.println("Populated " + numberfound + "<br>");
    				numberfound = 0;

	    			populated = true;
	        	} else { //Database populated
	    			while(wordResults.next()) //while we have results
	            	{   
	    				Integer found = Integer.valueOf(wordResults.getInt("urlid"));
	    				//out.println(found + ", ");
	    				if(foundIDs.containsKey(found)){ //The key exists in the list already
		    				//out.println("(IN LIST), ");
		    				numberfound++;
		    				commonIDsArray.add(found); // Add it to the common list
	    				} 
	    				//If it doesn't already exist in the table, we don't care;
	            	}
	    			foundIDs.clear(); //Wipe, repopulate with the arraylist results
	    			for(Integer watermelon : commonIDsArray){
	    				foundIDs.put(watermelon, watermelon);
	    			}
	    			commonIDsArray.clear();
	    			//out.println("In common: " + numberfound);
	        	}
				
			}
		}catch (SQLException e) {
        	e.printStackTrace();
        	out.println("WordQuery issues...<br>");
		}
        try {		
			String urlQuery = " ";        	
			String foundURL = " ";
			String desc = " ";
			int numResults = 1;
			for(Integer idsToPull : foundIDs.values())
        	{
        		//Retrieve by column name
    			urlQuery = "SELECT url, description FROM urls WHERE urlid='"+idsToPull+"' LIMIT 1";
    			//out.println("urlQuery: " + urlQuery + "<br>");
    			ResultSet urlResults = altstat.executeQuery(urlQuery);
    			urlResults.next();
    			foundURL = urlResults.getString("url");
    			desc = urlResults.getString("description");
        		out.println(numResults + ": <a href=\"" + foundURL + "\">" + foundURL + "</a><br>");
        		if(desc.length() > 0){
        			out.println("Description: " + desc + "<br>" + " " + "<br>");
        		} else {
        			out.println("No Description found! <br> <br>");
        		}       		

        		numResults++;
        	}
        } catch (SQLException e) {
        	e.printStackTrace();
        	out.println("Something messed up <br>");
			e.printStackTrace();
			out.println("Connection Failed <br>");
			out.println("SQLState: " + ((SQLException)e).getSQLState() + "<br>");
			out.println("Error Code: " + ((SQLException)e).getErrorCode() + "<br>");
			out.println("Message: " + (e).getMessage() + "<br>");
			Throwable t = e.getCause();
			while(t != null){
				out.println("Cause: " + t);
				t = t.getCause();
			}
        } //*/
        out.println("</body> \n </html>");
		
      }  
}
