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
public class MultiLevelScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public MultiLevelScheduler() {
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
    		System.out.println("dumping in log file");
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
	 	        System.out.println("Done");
	 	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
	 }
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
	  //  Lib.assertTrue(priorityWaitQueue.isEmpty());

	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    rearrangeThreads();
	    if (!this.topQueue.isEmpty()){
	    	ThreadState maxPrioritythreadS=this.topQueue.getFirst();
			this.topQueue.removeFirst();

	       if (maxPrioritythreadS.thread.getName().equals("main") ){
			    	printSystemStats();
			    }
	       recordScheduleInfo(maxPrioritythreadS);
	       System.out.println("nextThreadforexecution "+maxPrioritythreadS.thread.getName());
	       return maxPrioritythreadS.thread;
		}
		if (!this.middleQueue.isEmpty()){
			ThreadState maxPrioritythreadS=this.middleQueue.getFirst();
			this.middleQueue.removeFirst();

	       if (maxPrioritythreadS.thread.getName().equals("main") ){
			    	printSystemStats();
			    }
	       recordScheduleInfo(maxPrioritythreadS);
	       System.out.println("nextThreadforexecution "+maxPrioritythreadS.thread.getName());
	       return maxPrioritythreadS.thread;
		}
		if (!this.bottomQueue.isEmpty()){
			ThreadState maxPrioritythreadS=this.bottomQueue.getFirst();
			this.bottomQueue.removeFirst();

	       if (maxPrioritythreadS.thread.getName().equals("main")){
			    	printSystemStats();
			    }
	       recordScheduleInfo(maxPrioritythreadS);
	       System.out.println("nextThreadforexecution "+maxPrioritythreadS.thread.getName());
	       return maxPrioritythreadS.thread;
		}


	    return null;


	}

	public void recordScheduleInfo(ThreadState ThreadS ){

		//long currentTime=Machine.timer().getTime();
		long currentTime=SystemTime.getTime();
		String threadName=ThreadS.thread.getName();
		int currentPriority=ThreadS.priority;
		printLog(currentTime+","+threadName+","+currentPriority);
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
	avgWaitTime=(int)(avgWaitTime/totalThreads);
	avgTurnTime=(int)(avgTurnTime/totalThreads);

	    MultiLevelScheduler.printLog("System"+","+totalThreads+","+avgWaitTime+","+avgTurnTime+","+maxWaitTime);
	}



	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected KThread pickNextThread() {
		if (!this.topQueue.isEmpty()){
			return this.topQueue.getFirst().thread;
		}
		if (!this.middleQueue.isEmpty()){
			return this.middleQueue.getFirst().thread;
		}
		if (!this.bottomQueue.isEmpty()){
			return this.bottomQueue.getFirst().thread;
		}

		return null;
	}

	public void rearrangeThreads(){
		System.out.println("#########Call for rearrangeThreads############");
		int priority;

		   System.out.println("bottomQueue");
		   
		   Iterator<ThreadState> iterBottom= this.bottomQueue.iterator();
	       while(iterBottom.hasNext()){
	    	   ThreadState threadState=iterBottom.next();
	    	   priority=threadState.getEffectivePriority();
			   if (priority<=20 && priority >= 11 ){
				   iterBottom.remove();
				   this.middleQueue.add(threadState);
				   System.out.println("adding in middlequeue"+priority);
			   }else if (priority <=10 ){
				   iterBottom.remove();
				   this.topQueue.add(threadState);
			   }
		   }
		  
		   System.out.println("middleQueue");
             
		   Iterator<ThreadState> iterMiddile= this.middleQueue.iterator();
		   while(iterMiddile.hasNext()){
	    	   ThreadState threadState=iterMiddile.next();
			   priority=threadState.getEffectivePriority();
			   if (priority<=10 ){
				   iterMiddile.remove();
				   this.topQueue.add(threadState);
			   }else if (priority >20 ){
				   iterMiddile.remove();
				   this.bottomQueue.add(threadState);
			   }
		   }
		   
		   System.out.println("topQueue");
		   	
		   Iterator<ThreadState> iterTop= this.topQueue.iterator();
		   while(iterTop.hasNext()){
	    	   ThreadState threadState=iterTop.next();
			   priority=threadState.getEffectivePriority();
			   if (priority<=20 && priority >=11 ){
				   iterTop.remove();
				   this.middleQueue.add(threadState);
			   }else if (priority >20 ){
				   iterTop.remove();
				   this.bottomQueue.add(threadState);
			   }
		   }


		   for(ThreadState threadS: this.topQueue)
				System.out.println("All thread here in top "+"thread="+threadS.thread.getName()+" thread priority="+threadS.priority);
			for(ThreadState threadS: this.middleQueue)
				System.out.println("All thread here in middle "+"thread="+threadS.thread.getName()+" thread priority="+threadS.priority);
			for(ThreadState threadS: this.bottomQueue)
				System.out.println("All thread here in bottom "+"thread="+threadS.thread.getName()+" thread priority="+threadS.priority);


	}



	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	private LinkedList<ThreadState> topQueue =new LinkedList<ThreadState>();
	private LinkedList<ThreadState> middleQueue =new LinkedList<ThreadState>();
	private LinkedList<ThreadState> bottomQueue =new LinkedList<ThreadState>();
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
	//long temp = this.thread.soFarWaitTime;
	this.thread.soFarWaitTime=currentWaitTime+this.thread.lastWaitTimeForRun;

	// update the priority according to so far wait, run, age Time
	int newIncreasePriority=(int)((this.thread.soFarWaitTime-this.thread.soFarRunTime)/agingTime);
	this.priority=this.thread.startPriority-newIncreasePriority;  //increase priority (i.e. decrease priority absolute value)

	if (this.priority > priorityMaximum){ 
		this.priority=priorityMaximum;
	}
	if (this.priority < priorityMinimum){
		this.priority=priorityMinimum;
	}

	System.out.println(this.thread.getName()+" priority= "+this.priority+" soFarWaitTime="+this.thread.soFarWaitTime+" soFarRuntime="+this.thread.soFarRunTime+"  ");
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
		
		//put in the appropriate Queue according to its priority
		if ( this.priority >=1  && this.priority <=10)
				waitQueue.topQueue.add(this);
		if ( this.priority >=11  && this.priority <=20)
			waitQueue.middleQueue.add(this);
		if ( this.priority >20)
			waitQueue.bottomQueue.add(this);

		System.out.println("thread added="+this.thread.getName()+" thread priority="+this.priority+" at time="+System.currentTimeMillis());
		//start recording wait time of this thread
		
		this.thread.startWaitTime=SystemTime.getTime();
		
		
		
		for(ThreadState threadS: waitQueue.topQueue)
			System.out.println("*********All thread here top "+"thread="+threadS.thread.getName()+"thread priority="+threadS.priority);
		for(ThreadState threadS: waitQueue.middleQueue)
			System.out.println("*********All thread here middle "+"thread="+threadS.thread.getName()+"thread priority="+threadS.priority);
		for(ThreadState threadS: waitQueue.bottomQueue)
			System.out.println("*********All thread here bottom "+"thread="+threadS.thread.getName()+"thread priority="+threadS.priority);
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;


    	
    }
    
}
