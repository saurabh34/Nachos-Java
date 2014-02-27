package nachos.threads;

import java.io.*;

public class Log {
	public static File file = new File ("C:/dc/Project OS/Nachos-Java/DynamicPriorityScheduler.log");
	public static String data;
	public Log(String logData){
		data=logData;
	}
	
	 public static void main( String[] args )
	    {	
		 writeLog();
	    }
	 
	 public static void writeLog(){
		 
		 try{
	    		
	    		//File file =new File("javaio-appendfile.txt");
	 
	    		//if file doesn't exists, then create it
	    		if(!file.exists()){
	    			file.createNewFile();
	    		}
	 
	    		//true = append file
	    		    FileWriter fileWritter = new FileWriter(file.getName(),true);
	    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	    	        bufferWritter.write(data);
	    	        bufferWritter.close();
	 
		        System.out.println("Done");
	 
	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
		 
		 
	 }
 
}