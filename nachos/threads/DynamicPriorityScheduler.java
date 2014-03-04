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
/*
    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }
*/
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
		  try{//File file =new File("javaio-appendfile.txt");//if file doesn't exists, then create it
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
    
   public static File file = new File ("C:/dc/Project OS/Nachos-Java/DynamicPriorityScheduler.log");
  
   public static void printLog(String data){
	   writeLog(data+"\n");
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
	    Lib.assertTrue(priorityWaitQueue.isEmpty());
	    
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    ThreadState maxPriorityThread=maxPriorityThread();
	    
	    if (maxPriorityThread==null){
	    	return null;
	    }
	    System.out.println("nextThreadforexecution "+maxPriorityThread.thread.getName());
	    this.priorityWaitQueue.remove(maxPriorityThread);
	    recordScheduleInfo(maxPriorityThread);
	  
	    if (maxPriorityThread.thread.getName().equals("main") && this.priorityWaitQueue.isEmpty()){
	    	printSystemStats();
	    }
	    
	    return maxPriorityThread.thread;
	    
	    
	}
	
	public void recordScheduleInfo(ThreadState ThreadS ){
		
		long currentTime=Machine.timer().getTime();
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
	
	DynamicPriorityScheduler.printLog("System"+","+totalThreads+","+avgWaitTime+","+avgTurnTime+","+maxWaitTime);
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
		System.out.println("#########Call for maxPrioritythread############");
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
	
	
	long currentWaitTime=Machine.timer().getTime()-this.thread.startWaitTime;
	this.thread.soFarWaitTime=currentWaitTime+this.thread.lastWaitTimeForRun;
	
	int newIncreasePriority=(int)((this.thread.soFarWaitTime-this.thread.soFarRunTime)/agingTime);
	this.priority=this.priority-newIncreasePriority;
	
	if (this.priority > priorityMaximum){
		this.priority=priorityMaximum;
		}
		if (this.priority < priorityMinimum){
			this.priority=priorityMinimum;
		}
		
		System.out.println(this.thread.getName()+" priority:"+this.priority+"######"+this.thread.soFarWaitTime+" "+this.thread.soFarRunTime+"  ");
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
			this.thread.arrivalTime=Machine.timer().getTime();
		}
		
		if (this.thread.startRunTime>0){ 
			long currentRunTime=Machine.timer().getTime()-this.thread.startRunTime;
			this.thread.soFarRunTime=this.thread.soFarRunTime+currentRunTime;
		}
		
		waitQueue.priorityWaitQueue.add(this);
		System.out.println("thread added="+this.thread.getName()+"thread priority="+this.priority);
		this.thread.startWaitTime=Machine.timer().getTime();
		
		for(ThreadState threadS: waitQueue.priorityWaitQueue)
			System.out.println("*********All thread here "+"thread="+threadS.thread.getName()+"thread priority="+threadS.priority);
		
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	

    	
    }
    
}



