package nachos.threads;

import nachos.machine.*;

import java.util.*;
import java.io.*;
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
public class DynamicPriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public DynamicPriorityScheduler() {
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
    public long agingTime= Integer.parseInt(Config.getString("scheduler.agingTime"));
    //store waiting and turn around time of each thread
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
	    //get max priority thread
	    ThreadState maxPriorityThread=maxPriorityThread();

	    if (maxPriorityThread==null){
	    	return null;
	    }
	    //remove the thread from wait queue
	    this.priorityWaitQueue.remove(maxPriorityThread);
	    recordScheduleInfo(maxPriorityThread);
	   
	    //print statistics if its main method
	    if (maxPriorityThread.thread.getName().equals("main")){
	    	printSystemStats();
	    }

	    return maxPriorityThread.thread;


	}

	public void recordScheduleInfo(ThreadState ThreadS ){
        
		//print on console the max priority thread statistics 
		long currentTime=SystemTime.getTime();
		String threadName=ThreadS.thread.getName();
		int currentPriority=ThreadS.priority;
		printLog(currentTime+","+threadName+","+currentPriority);
	}

	public void printSystemStats(){
	
	//print system statistics
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
		avgWaitTime=(int)(avgWaitTime/totalThreads);
		avgTurnTime=(int)(avgTurnTime/totalThreads);

	    DynamicPriorityScheduler.printLog("System"+","+totalThreads+","+avgWaitTime+","+maxWaitTime+","+avgTurnTime);
	}



	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected KThread pickNextThread() {

		return maxPriorityThread().thread;
	}

	public ThreadState maxPriorityThread(){

		//iterate the max priority thread in linkedList
		//also iteration will give max priority thread whose waiting is maximum as it will in front
		//of every thread of same priority (first among them)
		ThreadState maxPriorityThread=null;
		if (this.priorityWaitQueue.isEmpty()){
			return null;
		}

		int maxPriorityOfThread=priorityMaximum;
		int priority;

		   for (ThreadState threadState: this.priorityWaitQueue){
			   priority=threadState.getEffectivePriority();
			   if (priority<maxPriorityOfThread){
				   maxPriorityThread=threadState;
			       maxPriorityOfThread=priority;
			   }
		   }

		return maxPriorityThread;
	}

	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}
	 public void reorderPriorityQueue(){
			
		}
	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	private LinkedList<ThreadState> priorityWaitQueue =new LinkedList<ThreadState>();
   
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

	//get so far waiting time
	long currentWaitTime=SystemTime.getTime()-this.thread.startWaitTime;
	this.thread.soFarWaitTime=currentWaitTime+this.thread.lastWaitTimeForRun;
	
	// update the priority according to so far wait, run, age Time
	int newIncreasePriority=(int)((this.thread.soFarWaitTime-this.thread.soFarRunTime)/agingTime);
	this.priority=this.thread.startPriority-newIncreasePriority;

	if (this.priority > priorityMaximum){
		this.priority=priorityMaximum;
		}
		if (this.priority < priorityMinimum){
			this.priority=priorityMinimum;
		}

		//System.out.println(this.thread.getName()+" priority= "+this.priority+" soFarWaitTime="+this.thread.soFarWaitTime+" soFarRuntime="+this.thread.soFarRunTime+"  ");
	    return this.priority;
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
        
		//first time added to wait thread, record arrival time
		if (this.thread.arrivalTime==0){
			this.thread.arrivalTime=SystemTime.getTime();
		}
		//add previous run time and update so far run time
		if (this.thread.startRunTime>0){ 
			long currentRunTime=SystemTime.getTime()-this.thread.startRunTime;
			this.thread.soFarRunTime=this.thread.soFarRunTime+currentRunTime;
		}
		//put in the waitQueue 
		waitQueue.priorityWaitQueue.add(this);
		
		//start recording wait time of this thread
		this.thread.startWaitTime=SystemTime.getTime(); 

		
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;


    	
    }
    
}

