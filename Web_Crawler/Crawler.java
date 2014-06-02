/*Standard imports*/
import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.sql.*;
import java.util.*;

/*Jsoup*/
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler
{
	public static Connection connection;
	public static int urlID;
	public static int scannedURLS;
	public static int wordsRipped;
	public static int maxUrls;
	String domain;
	public Properties props;
	public static Statement stat; //new

	Crawler() {
		urlID = 0;
		scannedURLS = 0;
		wordsRipped = 0;
		domain = "website.com";
	}


	public void readProperties() throws IOException {
      		props = new Properties();
      		FileInputStream in = new FileInputStream("database.properties");
      		props.load(in);
      		in.close();
	}

	// Opens the connection to the Server
	public void openConnection() throws SQLException, IOException
	{
		System.out.print("Opening connection...\n");
		String drivers = props.getProperty("jdbc.drivers");
      	if (drivers != null) System.setProperty("jdbc.drivers", drivers);

      	String url = props.getProperty("jdbc.localurl");
      	//String url = props.getProperty("jdbc.remoteurl");      	
      	String username = props.getProperty("jdbc.username");
      	System.out.println("UN: " +username);
      	String password = props.getProperty("jdbc.password");
      	System.out.println("PW: " +password);
      	maxUrls = Integer.parseInt(props.getProperty("crawler.maxurls"));
      	System.out.println("MAX: " +maxUrls);      	

      	System.out.print("Connection line...\n");
		connection = DriverManager.getConnection(url, username, password);
   	}

   	//Creates the SQL Database
	public void createDB() throws SQLException, IOException {
		openConnection();

        stat = connection.createStatement();

		// Delete the table first if any
		try {
			stat.executeUpdate("DROP TABLE URLS");
			stat.executeUpdate("DROP TABLE WORDS");
		}
		catch (Exception e) {
		}
		
		// Create the tables
        stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200))");
        stat.executeUpdate("CREATE TABLE WORDS (word VARCHAR(200), urlid INT, PRIMARY KEY(word, urlid))");
        System.out.print("Database created...\n");

	}

	//Checks to see if the URL is in the Database
	public boolean urlInDB(String urlFound) throws SQLException, IOException {
        //Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM urls WHERE url LIKE '"+urlFound+"' LIMIT 1");

		if (result.next()) {
	        //System.out.println("URL "+urlFound+" already in DB");
			return true;
		}
	    //System.out.println("URL "+urlFound+" not yet in DB");
		return false;
	}

	//Obsolete code that should be deleted.
	public boolean wordInDB(int ident, String wordToCheck) throws SQLException, IOException {
       //Statement stat = connection.createStatement();
		System.out.print("THIS SHOULDN'T BE RUNNING\n");
		ResultSet result = stat.executeQuery( "SELECT * FROM words WHERE word LIKE '"+wordToCheck+"' AND urlid LIKE '"+ident+"'");

		if (result.next()) {
	        //System.out.println("URL "+urlFound+" already in DB");
			return true;
		}
	    //System.out.println("URL "+urlFound+" not yet in DB");
		return false;
	}

	/*Throw URL into DB*/
	public void insertURLInDB(String url) throws SQLException, IOException {
        //Statement stat = connection.createStatement();
		String query = "INSERT INTO urls VALUES ('"+urlID+"','"+url+"','')";
		//System.out.println("Executing "+query);
		stat.executeUpdate(query);
		//System.out.print("Update the urlID ["+urlID+"]\n");
		urlID++;
		//System.out.print("Updated the urlID ["+urlID+"]\n");
	

	}

	/*Put a word into the DB*/
	public void insertWORDInDB(String values) throws SQLException, IOException {
        try{
        	//Statement stat = connection.createStatement();
			//String query = "INSERT IGNORE INTO words VALUES ('"+word+"','"+idToInsert+"')";
			String query = "INSERT IGNORE INTO words VALUES "+values;
			
			//System.out.println("Executing "+query);
			stat.executeUpdate( query );
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	/*Throw the description into the database*/
	public void instertDESCInDB(int tofind, String desc) throws SQLException, IOException {

    	try{
    		//Statement stat = connection.createStatement();
			String query = "UPDATE urls SET description='"+desc+"' WHERE urlid='"+tofind+"' LIMIT 1";
			//System.out.println("Executing "+query);
			stat.executeUpdate( query );
		}catch(Exception e){
				System.out.println(e.getMessage());
		}	

	}

	/*Fetch a url from the db*/
	public String fetchURLfromDB(int id) throws SQLException, IOException {
    	try {
    		//System.out.print("Fetching URL from DB at "+id+"\n");
    		//Statement stat = connection.createStatement();
			String query = "SELECT url FROM urls WHERE urlid='"+id+"' LIMIT 1";
			//System.out.print("Query: " +query);
			ResultSet result = stat.executeQuery(query);
					
			String foundURL;
			if (result.next()) {
				foundURL = result.getString(1);
	        //	System.out.println("URL "+foundURL+" found in DB");
				return foundURL;
			} 
		} catch(Exception e){
			System.out.println(e.getMessage());			
		}
		/*Quick and dirty workaround, didn't really impact results*/
		String failure = "http://www.cs.purdue.edu";
		return failure; 
	}

	/*Make url complete*/
	public String makeAbsoluteURL(String url, String parentURL) {
		if (url.indexOf(":") < 0) {
			// the protocol part is already there.
			return url;
		}

		if (url.length() > 0 && url.charAt(0) == '/') {
			// It starts with '/'. Add only host part.
			int posHost = url.indexOf("://");
			if (posHost < 0) {
				return url;
			}
			int posAfterHost = url.indexOf("/", posHost+3);
			if (posAfterHost < 0) {
				posAfterHost = url.length();
			}
			String hostPart = url.substring(0, posAfterHost);
			return hostPart + "/" + url;
		} 

		// URL start with a char different than "/"
		int pos = parentURL.lastIndexOf("/");
		int posHost = parentURL.indexOf("://");
		if (posHost <0) {
			return url;
		}
		return url;	
	}

	//Jsoup rip the URLs 'quick' and easy
	public void souperURLRipper(int idtorip){
		try {
			
			String url = fetchURLfromDB(idtorip);
			//System.out.print("URL: " +url);
			Document toRip = Jsoup.connect(url).ignoreContentType(true).get();
			Elements links = toRip.select("a[href]");

			for (Element link : links) {
				String linkFound = link.attr("abs:href").toString();
				linkFound = makeAbsoluteURL(linkFound, url);
				if(linkFound.contains(domain) && linkFound.contains("http") && (urlID < maxUrls)){
        	    	//System.out.print("Link found: " +linkFound);
        	    	if(!linkFound.contains(".pdf") && !linkFound.contains(".mp3") && !linkFound.contains(".jpg")){
	        	    	if (!urlInDB(linkFound)) {
							insertURLInDB(linkFound);
						}	
        	    	}
        		}
        	}
		
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	/**/
	public void souperWordRipper(int idtorip)
	{
		try {

			String url = fetchURLfromDB(idtorip);
			Document toRip = Jsoup.connect(url).get();
			Element words = toRip.body();
			String wordString = words.text();
			String wordsarray[] = wordString.split(" ");
			String description = toRip.title().replaceAll("[^\\dA-Za-z\\s]", ""); //This could probably be optimized. 
			System.out.println("Title: "+description);
			/*This could be simplified at some point*/
			if(description.length() < 5){
				description = toRip.head().text().replaceAll("[^\\dA-Za-z\\s]", "");
				if(description.length() < 5){	
					if(wordString.length() < 200){
						description = wordString.replaceAll("[^A-Za-z\\s]", " ");
						System.out.println("WordString below 200"+description);
					} else {
						description = wordString.replaceAll("[^A-Za-z\\s]", " ").substring(0, 199);	
						System.out.println("WordString above 200"+description);
					}
				}
			} else if(description.length() >= 200) {
				description = description.substring(0, 199);	
			}

			//Throw them into the database
			instertDESCInDB(idtorip, description);
			System.out.print("D");
			String query = "";
			int insertions = 0;
			for(String someword : wordsarray){
				someword = someword.replaceAll("[^A-Za-z]", "").toLowerCase();
				if(someword.length() > 0){
					query = query+"('"+someword+"','"+idtorip+"'),";
					insertions++;
				}
			}//*/
			query = query.substring(0, query.length() - 1);
			insertWORDInDB(query);

		
		} catch (Exception e){
			e.printStackTrace();
		}

	}

   	public static void main(String[] args)
   	{
		Crawler crawler = new Crawler();

		try {
			/*Initial setup of the database*/
			crawler.readProperties();
			String root = crawler.props.getProperty("crawler.root");
			crawler.createDB();
			//crawler.fetchURL(root);
			crawler.insertURLInDB(root);
			//System.out.print("Ripping from URL\n");
			System.out.println("Entering the ripper...");
			crawler.souperURLRipper(0);
			scannedURLS++;
			//System.out.print("S:["+scannedURLS+"]F:{"+urlID+"}");			
			while((scannedURLS < maxUrls) && (urlID < maxUrls)){
				crawler.souperURLRipper(scannedURLS);
				System.out.print("S:["+scannedURLS+"]F:{"+urlID+"}");
				scannedURLS++;
			}
			System.out.println("MaxURLs reached... scanning in words");
			while(wordsRipped < maxUrls){
				crawler.souperWordRipper(wordsRipped);
				System.out.print("W:["+wordsRipped+"]");
				wordsRipped++;
			}//*/

		}
		catch( Exception e) {
         		e.printStackTrace();
		}
    }
}

	
