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

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 32;    

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
	   	long currentWaitTime=Machine.timer().getTime()-maxPriorityThread.startWaitTime;
	    maxPriorityThread.soFarWaitTime=maxPriorityThread.soFarWaitTime+currentWaitTime;
	    maxPriorityThread.startRunTime=Machine.timer().getTime();
	    this.priorityWaitQueue.remove(maxPriorityThread);
    	return maxPriorityThread.thread;
	    
	    
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
		ThreadState maxPriorityThread=null;
		if (this.priorityWaitQueue.isEmpty()){
			return null;
		}
		
		int maxPriorityOfThread=priorityMinimum;
		int priority;
		
		   for (ThreadState threadState: this.priorityWaitQueue){
			   priority=threadState.getEffectivePriority();
			   if (priority> maxPriorityOfThread)
				   maxPriorityThread=threadState;
			       maxPriorityOfThread=priority;
			       
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
	//create the priority queue
	//private TreeMap<Integer,LinkedList<ThreadState>> priorityWaitQueue=new TreeMap<Integer,LinkedList<ThreadState>>();
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
	long timeInterval=100; 
	
	long currentWaitTime=Machine.timer().getTime()-this.startWaitTime;
	this.soFarWaitTime=currentWaitTime+this.soFarWaitTime;
	int newPriority=(int)((this.soFarWaitTime-this.soFarRunTime)/timeInterval);
	   //System.out.println("######"+this.soFarWaitTime+" "+this.soFarRunTime+"  "+newPriority);
		if (newPriority > priorityMaximum){
			newPriority=priorityMaximum;
		}
		if (newPriority < priorityMinimum){
			newPriority=priorityMinimum;
		}
		this.priority=this.priority+newPriority;
		System.out.println(this.thread.getName()+" priority:"+this.priority+"######"+this.soFarWaitTime+" "+this.soFarRunTime+"  ");
		   try {
		        System.in.read();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		
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
		
		if (this.startRunTime>0){ //assuming thread don't go in IO blocking state
			long currentRunTime=Machine.timer().getTime()-this.startRunTime;
			this.soFarRunTime=this.soFarRunTime+currentRunTime;
		}
		
		waitQueue.priorityWaitQueue.add(this);
		this.startWaitTime=Machine.timer().getTime();
		for(ThreadState threadS: waitQueue.priorityWaitQueue)
			System.out.println("********"+threadS.thread);
		
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	private long soFarWaitTime=0;
    private long soFarRunTime=0;
    private long startWaitTime=0;
    private long startRunTime=0;
    
  
    	
    	
    }
    
}



