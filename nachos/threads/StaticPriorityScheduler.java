package nachos.threads;

import nachos.machine.*;
import nachos.threads.MultiLevelScheduler.ThreadState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class StaticPriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public StaticPriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
    

	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);

	getThreadState(thread).setPriority(priority);

    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }
    
    public static void writeLog(String data){
		
	  try{  File file = new File (inp_file);
    		if(!file.exists()){
    			file.createNewFile();
    			
    		}
    		if (openFileFirstTime){
    			FileWriter fileWritter = new FileWriter(file.getName());
    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
    	        bufferWritter.write("Starting clock Time (Baseline in milliseconds)= "+SystemTime.baslineTime+"\n");
    	        bufferWritter.close();
    			openFileFirstTime=false;
    		}
     		//true = append file
    		    FileWriter fileWritter = new FileWriter(file.getName(),true);
    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
    	        bufferWritter.write(data);
    	        bufferWritter.close();
 	      
 	    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    //create log file
    public static boolean openFileFirstTime=true;
    public static final String inp_file= Config.getString("statistics.logFile");
   // public static File file = new File (inp_file);

    
    
    public static void printLog(String data){
    	if (inp_file != null){
    		writeLog(data+"\n");
    	}
    	System.out.println(data);
    }
     
    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = Integer.parseInt(Config.getString("scheduler.maxPriority"));
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.parseInt(Config.getString("scheduler.maxPriority"));    
    public static ArrayList<Long> threadsWaitingTime=new  ArrayList<Long>();
    public static ArrayList<Long> threadsTurnAroundTime=new  ArrayList<Long>();
    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void ChangeLockOwnerOfWaitingThreadsTo(KThread newLockOwner){}
	 public LinkedList<Integer> getThreadIdsOfLock(){
		 return null;
	 };
	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);

	}

	public void acquire(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
	    Lib.assertTrue(priorityWaitQueue.isEmpty());

	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    if (priorityWaitQueue.isEmpty())
	    	return null;
	    int highestKey=priorityWaitQueue.firstKey(); //get highest priority thread (i.e. minimum absolute value)


	    //remove highest priority thread and if its only one, remove the key also
	    if (priorityWaitQueue.get(highestKey).size()==1){ 
	    	KThread thread =priorityWaitQueue.get(highestKey).removeFirst();
	    	priorityWaitQueue.remove(highestKey);

	    	long currentWaitTime=SystemTime.getTime()-thread.startWaitTime;
	    	thread.soFarWaitTime=currentWaitTime+thread.lastWaitTimeForRun;
	    	if (thread.getName()=="main" ){
		    	printSystemStats();
		    }
	    	recordScheduleInfo(thread);
	    	return thread;
	      }

	    KThread thread =priorityWaitQueue.get(highestKey).removeFirst();
	    long currentWaitTime=SystemTime.getTime()-thread.startWaitTime;
    	thread.soFarWaitTime=currentWaitTime+thread.lastWaitTimeForRun;
    	
        if (thread.getName()=="main"){
	    	printSystemStats();
	    }
        
        recordScheduleInfo(thread);
	    return thread;


	}
	
	public void recordScheduleInfo(KThread thread ){

		long currentTime=SystemTime.getTime();
		String threadName=thread.getName();
		int currentPriority=thread.inherentPriority;
		printLog(currentTime+","+threadName+","+currentPriority);
	}

	

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected KThread pickNextThread() {

		 if (priorityWaitQueue.isEmpty())
		    	return null;
		 int highestKey=priorityWaitQueue.firstKey();
		 return priorityWaitQueue.get(highestKey).getFirst();
	}

	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	public void printSystemStats(){
	int totalThreads=threadsWaitingTime.size();
	long maxWaitTime=0;
	long avgWaitTime=0;
	long avgTurnTime=0;
	for (int i=0;i<totalThreads;i++){
		avgWaitTime=avgWaitTime+threadsWaitingTime.get(i);
		avgTurnTime=avgTurnTime+threadsTurnAroundTime.get(i);
		if (threadsWaitingTime.get(i) >maxWaitTime ){
			maxWaitTime=threadsWaitingTime.get(i);
		}

	}
	if(totalThreads >0) {
		avgWaitTime=(int)(avgWaitTime/totalThreads);
		avgTurnTime=(int)(avgTurnTime/totalThreads);
	}

	StaticPriorityScheduler.printLog("System"+","+totalThreads+","+avgWaitTime+","+maxWaitTime+","+avgTurnTime);
	}

    public void reorderPriorityQueue(){
    	System.out.println("Starting reordring...");
    	System.out.println(priorityWaitQueue);
    	LinkedList<KThread> tempThreads=new LinkedList<KThread>();
    	
    	for(Iterator<Map.Entry<Integer,LinkedList<KThread>>> it = priorityWaitQueue.entrySet().iterator(); it.hasNext(); ) {
    	      Map.Entry<Integer,LinkedList<KThread>> entry = it.next();
    	      LinkedList<KThread> priorityLevelThreads=entry.getValue();
    	      //System.out.println("priorityLevelThreads.size()== "+priorityLevelThreads.size());
    	      Iterator<KThread> iter= priorityLevelThreads.iterator();
    	      
    	      while(iter.hasNext()){
    	    	  KThread thread=iter.next();
    	    	  if (getThreadState(thread).getPriority() != thread.inherentPriority){
    	    		  getThreadState(thread).priority=thread.inherentPriority;
    	    		  tempThreads.add(thread);
    	    		  iter.remove();
    	    		 
    	    	  }
    	       }
    	      
    	     // System.out.println("numOfThreadsRemoved== "+numOfThreadsRemoved);
    	    //  System.out.println("priorityLevelThreads.size()== "+priorityLevelThreads.size());
    	      if (priorityLevelThreads.size()== 0){
  	        	   it.remove();
    	      }
    	    }
    	
    	Iterator<KThread> iter= tempThreads.iterator();
    	 while(iter.hasNext()){
    		 KThread thread=iter.next();
    		 if (!priorityWaitQueue.containsKey(getThreadState(thread).priority)){ //check priority level exist or not
    				priorityWaitQueue.put(getThreadState(thread).priority,new LinkedList<KThread>()); //else create one
    			    }

    			priorityWaitQueue.get(getThreadState(thread).priority).add(thread);
    		 
    	 }
    	 
    	System.out.println("Finished reordring...");
     	System.out.println(priorityWaitQueue);
     	
    	
    }
	
    public int getMaxThreadPriority(){
    	return -1;
    }
	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
   
    public boolean transferPriority;
	//create the priority queue
	private TreeMap<Integer,LinkedList<KThread>> priorityWaitQueue=new TreeMap<Integer,LinkedList<KThread>>();
   
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;

	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {

	    return priority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
        this.priority = priority;
        this.thread.originalPriority = priority;
        this.thread.inherentPriority = priority;
        this.thread.startPriority=priority;
	}
   
	
	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */

	public void waitForAccess(PriorityQueue waitQueue) {

		if (this.thread.arrivalTime==0){
			this.thread.arrivalTime=SystemTime.getTime();
		}

		if (this.thread.startRunTime>0){ 
			long currentRunTime=SystemTime.getTime()-this.thread.startRunTime;
			this.thread.soFarRunTime=this.thread.soFarRunTime+currentRunTime;
		}


		//add thread in TreeMap according to their priority
		if (!waitQueue.priorityWaitQueue.containsKey(this.priority)){ //check priority level exist or not
			waitQueue.priorityWaitQueue.put(this.priority,new LinkedList<KThread>()); //else create one
		    }

		waitQueue.priorityWaitQueue.get(this.priority).add(thread); //add thread at that priority level
		//start recording wait time of this thread
		this.thread.startWaitTime=SystemTime.getTime();

		
	}
	
	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    }
    
   
    
    
}
