package uk.ac.le.cs.CO3090.cw1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

//import uk.ac.le.cs.CO3090.cw1.WebBot;


/**
 * @author Yi Hong
 * 
 *  CO3090/CO7090 CW 1  
 *
 *  It is not necessary to understand every 
 *  method implemented here but you should 
 *  know how to use them. 
 *  
 *  You are free to make any changes to this
 *  class though you don't have to.
 *    
 */

public class WebExtractor {
	
	/* Regular expression for  */
	public static final Pattern link_pattern=Pattern.compile("(?i)<a([^>]+)>(.+?)</a>");
	public static final Pattern href_pattern=Pattern.compile("\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))");
	
	
	/**
	 * 
	 * returns the HTML source code of a web page
	 * 
	 * @param  address: "base" page or seed page URL
	 *         e.g. http://www.bbc.co.uk/news  
	 *         
	 *         IMPORTANT:
	 *         
	 *         Please DO NOT use any University web page 
	 *         for testing as this may triggered DDoS attack 
	 *         alert at IT Service. Any sample URL provided 
	 *         here is purely for information purposes only.
	 *                    
	 * @return the HTML source code of the given web page
	 *          
	 */
	public static String getTextFromAddress(String address){
		
		 URL url;
		    InputStream is = null;
		    BufferedReader br;
		    String line;

		    try {
		        url = new URL(address);
		        is = url.openStream();  
		        br = new BufferedReader(new InputStreamReader(is));

		        StringBuffer sb=new StringBuffer(); 
		        
		        while ((line = br.readLine()) != null) {
		        	sb.append(line);
		        	sb.append("\n");
		        }
		        
		        String content=sb.toString();
		        return content;
		        
		    }  catch (MalformedURLException mue) {
		    	//ignore dead links
		    	//mue.printStackTrace();
		    } catch (IOException ioe) {
		    	//ignore IO error
		    	//ioe.printStackTrace();
		    } finally {
		        try {
		            if (is != null) is.close();
		        } catch (IOException ioe) {
		            ioe.printStackTrace();
		            System.out.println(ioe.toString());
		        }
		    }
		    return "";
		
	}
	
	/**
	 *  
	 * Extracts all internal hyperlinks on a given page.
	 * 
	 *  For example: 
	 *  Suppose http://example.com/some.html 
	 *  has the following content:  
	 * 
	 *  <HTML>
	 *      <b>A sample page with 3 links</b>
	 *      <a href="1.html">link1</a>
	 *      <a href="http://example.com/other.html">other page</a>
	 *      <a href="http://www.google.com">Google</a>
	 *  </HTML>
	 *  
	 *  
	 *  extractHyperlinks ("http://example.com/some.html", html_source_text) 
	 *  
	 *  will return:
	 *  
	 *     http://example.com/1.html 
	 *     http://example.com/other.html 
	 *     
	 *     *Note 
	 *     http://www.google.com is not returned because it is
	 *     located in another host)
	 *  
	 * @param address: a URL 
	 * @param html source code 
	 * @return A list of "internal" hyperlinks  
	 */
	

	
	public static ArrayList<String> extractHyperlinks(String address, String content){
		
		ArrayList<String> ret=new ArrayList<String>();
		
		try{
			URL  url = new URL(address);
			Matcher matcher = link_pattern.matcher(content);
			
         while (matcher.find()) {
        	
        	 String links = matcher.group(1);  
        	 Matcher hrefs = href_pattern.matcher(links);
        	 
             while(hrefs.find()){
            	String link = hrefs.group(1).replaceAll("'", "").replaceAll("\"", "");
            	String absolutePath="";
            	String path=url.getPath();
        	    if ((path == null) || path.equals("") || path.equals("/")){
        	    	absolutePath="";
        	    }
        	    int lastSlashPos = path.lastIndexOf('/');
        	    if (lastSlashPos >= 0){
        	    	absolutePath=path.substring(0, lastSlashPos); //strip off the slash
        	    }else{
        	    	absolutePath= ""; 
        	    }

            	String addr="";     	
            	URI uri=new URI(link);  	
            	if(!uri.isAbsolute()){	            	
            		addr=url.getProtocol()+"://"+url.getHost()+absolutePath+"/"+link;
            		ret.add(addr);
            	}else{
            		if(link.contains(url.getHost())){
            		if(!link.contains("mailto:")){
            			addr=link;
            			ret.add(addr);
            		}
            		}
            	}
            } 
        
		}
            
		}catch(Exception ex){
			System.out.println(ex.toString());
		}
        return ret;
} 
	
	
	/**
	 * 
	 * Strips HTML tags from a string
	 * 
	 * @param html
	 * @return string (all HTML tags removed will be removed)
	 * 
	 */
	public static String getPlainText(String html){
		String text="";
		try{
			text= Jsoup.parse(html).text();
		}catch(Exception ex){
			return "";
		}
		return text;
	}
	
	
	
	/**
	 *  count the occurrences of each letter
	 * 
	 * @param text
	 * @return return a HashMap storing the occurrences of each letter in the text
	 */
	public static HashMap<Character, Integer> calculate(String text){ 
		 
		 HashMap<Character, Integer> map=new HashMap<Character, Integer>();
		 
		 for(int i=0;i<WebBot.ALPHABET.length();i++){
				map.put(WebBot.ALPHABET.charAt(i), 0);
		  }
		 
			for (int i = 0; i < text.length(); ++i){
			    char charAt = text.toLowerCase().charAt(i);	 
			    if (map.containsKey(charAt)){
			    	if(text.contains(String.valueOf(charAt).toLowerCase())){
				    	map.put(charAt, map.get(charAt) + 1);
				    }	
			    }
			 }
			
			return map;
	}
	
	
	
	public static void main(String args[]){
		
		/*
		 * 
		 * Demo
		 * 
		 * IMPORTANT:
		 * 
		 * Please DO NOT use any University webpage 
	     * for testing as this may triggered a DoS (Denial-of-service) 
	     * attack alert at IT Service. Any sample URL provided 
		 * below is purely for information purposes only.
		 */

		String websiteURL="http://www.bbc.co.uk/news"; 
		
		//get HTML 
		String content=WebExtractor.getTextFromAddress(websiteURL);

		//get all links
		ArrayList<String> urls=WebExtractor.extractHyperlinks(websiteURL, content);
		
		//print all links
		 for(String link:urls){
			 System.out.println("Links:"+link);
		 }
		
		//strips HTML tags
		String text=WebExtractor.getPlainText(content);
		
		//print text
		System.out.println(text);
		
		
		//print character count
		System.out.println(calculate(text));
		


	}
	
		
	}
		
