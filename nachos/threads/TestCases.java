package nachos.threads;
import nachos.machine.*;


public class TestCases {

	public static void main(String[] args) {
	}
	
	public static void runTestCase1(){
	KThread low     = new KThread(new LowPriorityThread()).setName("Low1");
    KThread med     = new KThread(new MediumPriorityThread()).setName("Med1");
    KThread high    = new KThread(new HighPriorityThread()).setName("High1");
    KThread high2    = new KThread(new HighPriorityThread()).setName("High2"); 
   
   
    boolean oldInterrupStatus = Machine.interrupt().disable();
    ThreadedKernel.scheduler.setPriority(low, 22);
    ThreadedKernel.scheduler.setPriority(med, 13);
    ThreadedKernel.scheduler.setPriority(high, 5);
    ThreadedKernel.scheduler.setPriority(high2, 4);
    Machine.interrupt().restore(oldInterrupStatus);
  
    high2.fork();
    high.fork();
    med.fork();
    low.fork();
	}
	
	 private static final char dbgThread = 't';
	    
	    private static class HighPriorityThread implements Runnable
	    { 	
	    	public void run()
	        {     
	    		for(int j=0;j<5;j++){
	                for(int i = 0; i < 60; ++i)
	                {
	                	Lib.debug(dbgThread, "$$$ HighPriorityThread running, i = " + i);
	                }
	             boolean oldInterrupStatus = Machine.interrupt().disable();
	             KThread.currentThread().yield();
	             Machine.interrupt().restore(oldInterrupStatus);
	    		}
	               
	        }
	       
	    }
	   
	    private static class MediumPriorityThread implements Runnable
	    {
	       public void run()
	        {
	          for (int j=0;j<10;j++) {   
	        	for(int i = 0; i < 10; ++i)
	                {
	                        Lib.debug(dbgThread, "$$$ MediumPriorityThread running, i = " + i);
	                }
	          //  KThread.currentThread.yield();
	            }
	         }
	        
	    }
	   
	    private static class LowPriorityThread implements Runnable
	    {
	    	public void run()
	    	 {
	                for(int i = 0; i < 100; ++i)
	                {
	                	Lib.debug(dbgThread, "$$$ LowPriorityThread running, i = " + i);
	                }
	        }
	    }
	
}
