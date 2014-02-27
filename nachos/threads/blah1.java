package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities
 * with implemented aging.
 *
 * <p>
 * The dynamic priority differs from the static priority
 * scheduler in that aging occurs at an interval specified
 * in the config file. At each aging interval, the last-run
 * thread is demoted and all other (waiting) threads are
 * promoted. The queue is then sorted according to these
 * new effective priorities.
 * <p>
 * Again, this class is based on the Priority Scheduler
 * class in this package.
 * <p>
 * Written by Jody Zeitler and Frank Markson
 */
public class blah1 extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public blah1() {
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
		startTime = new Date();
		lastAge = new Date();
		return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		boolean intStatus = Machine.interrupt().disable();
		       
		Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
		getThreadState(thread).setPriority(priority);

		Machine.interrupt().restore(intStatus);
    }

	public void setEffectivePriority(KThread thread, int priority) {
		boolean intStatus = Machine.interrupt().disable();
		       
		Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
		getThreadState(thread).setEffectivePriority(priority);

		Machine.interrupt().restore(intStatus);
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

	public void modifyEffectivePriority(KThread thread, int value) {
		int priority = getEffectivePriority(thread);
		int newPriority = priority + value;
		if (newPriority < priorityMinimum) newPriority = priorityMinimum;
		if (newPriority > priorityMaximum) newPriority = priorityMaximum;
		if (newPriority == priority) return;

		boolean intStatus = Machine.interrupt().disable();

		setEffectivePriority(thread, newPriority);

		Machine.interrupt().restore(intStatus);
	}

	/* Priority value upon thread creation */
    public static final int priorityDefault = 1;
	/* Minimum priority - should be 0 always */
    public static final int priorityMinimum = 0;
	/* Max priority value parsed from config file */
    public static final int priorityMaximum = Integer.parseInt(Config.getString("scheduler.maxPriority"));

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

	/* aging time of the queue, specified in the config file */
	public static final int agingTime = Integer.parseInt(Config.getString("scheduler.agingTime"));
	/* when the queue was initialized */
	protected Date startTime;
	/* when the last aging of the queue occurred */
	protected Date lastAge;

	/**
	 * Check whether or not the age interval has been reached
	 * and return the number of intervals that have passed.
	 */
	protected int checkBirthday() {
		Date newAge = new Date();
		long diff = newAge.getTime() - lastAge.getTime();
		if (diff > agingTime) {
			lastAge = newAge;
			return (int) diff/agingTime;
		} else {
			return 0;
		}
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
	    	getThreadState(thread).acquire(this);
		}

		/**
		 * <p>
		 * Removes the first element in the queue.
		 * <tt>waitForAccess()</tt> guarantees the first thread
		 * will have the highest priority in the queue. A message
		 * in the form <time(ms), thread-id, priority> is printed
		 * each time a thread is scheduled.
		 * <p>
		 * Since this method is called each time a context-switch
		 * occurs, it also updates the run and wait times of each
		 * thread in the queue and checks if the last-run thread
		 * has finished execution.
		 * <p>
		 * If the aging interval has been reached, the running thread
		 * is demoted and the waiting threads are promoted. The queue
		 * is then sorted according to these new priorities before
		 * picking a new thread.
		 * <p>
		 * When the main thread is scheduled, the stats are calculated
		 * and printed out.
		 *
		 * @return	the first thread in the queue
		 */
		public KThread nextThread() {
	    	Lib.assertTrue(Machine.interrupt().disabled());
			if (waitQueue.isEmpty())
				return null;

			if (lastScheduled != null) { // at least 1 thread has already been scheduled
				boolean departed = true;
				int units = checkBirthday(); // get number of times the aging time has expired
				//if (units > 0) System.out.println("Threads aging...");
				ListIterator<KThread> iter = waitQueue.listIterator();
				while (iter.hasNext()) {
					ThreadState element = getThreadState(iter.next());
					/* don't modify the main thread */
					if (element.thread.getName() == "main") continue;

					if (lastScheduled == element.thread) { // running
						departed = false; // thread still in queue
						if (units > 0) modifyEffectivePriority(element.thread, 1); // lower priority
						element.updateRunTime(); // update run time
						//System.out.println(element.thread.toString() + " is running; effective priority is now " + element.getEffectivePriority());
					}

					else if (element.thread.getStatus() == 1) { // ready & waiting
						if (units > 0) modifyEffectivePriority(element.thread, -1); // higher priority
						element.updateWaitTime(); // update wait time
						//System.out.println(element.thread.toString() + " is waiting; effective priority is now " + element.getEffectivePriority());
					}
				}
				if (units > 0) Collections.sort(waitQueue, new PriorityComparator()); // sort queue by effective priority

				if (departed == true) { // last thread finished execution
					ThreadState state = getThreadState(lastScheduled);
					state.updateRunTime();
					state.setDepartureTime();

					/* add to stats */
					totalWaitTime += state.waitTime;
					if (state.waitTime > maxWaitTime) maxWaitTime = state.waitTime;
					totalTurnaroundTime += state.departureTime.getTime() - state.arrivalTime.getTime();
				}
			}

			/* select next thread */
			lastScheduled = waitQueue.removeFirst();

			/* print out stats */
			ThreadState state = getThreadState(lastScheduled);
			System.out.println("<"+((new Date()).getTime()-startTime.getTime())+", "+lastScheduled.getId()+", "+state.getEffectivePriority()+">");

			/* calculate stats when main is scheduled */
			if (lastScheduled.getName() == "main") {
				long threadCount = (long) KThread.getThreadCount();
				System.out.println("\nNumber of threads: " + threadCount);
				System.out.println("Average wait time: " + (totalWaitTime/threadCount) + "ms");
				System.out.println("Average turnaround time: " + (totalTurnaroundTime/threadCount) + "ms");
				System.out.println("Maximum waiting time: " + maxWaitTime + "ms");
			}

			return lastScheduled;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected KThread pickNextThread() {
			if (waitQueue.isEmpty())
				return null;
			return waitQueue.getFirst();
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

		protected LinkedList<KThread> waitQueue = new LinkedList<KThread>(); // list of waiting threads
		private KThread lastScheduled = null; // last thread picked by nextThread()

		private long totalWaitTime = 0;
		private long maxWaitTime = 0;
		private long totalTurnaroundTime = 0;

		/* Comparator that compares KThreads based on their effective priorities */
		private class PriorityComparator implements Comparator<KThread>{
			public int compare(KThread t1, KThread t2) {
				int p1 = getThreadState(t1).getEffectivePriority();
				int p2 = getThreadState(t2).getEffectivePriority();
				return p1 > p2 ? 1 : (p1 < p2 ? -1 : 0);
			}
		}
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
			setEffectivePriority(priorityDefault);

			this.arrivalTime = new Date();
			this.lastUpdate = new Date();
		}

		public ThreadState(KThread thread, int priority) {
			this.thread = thread;
			setPriority(priority);
			setEffectivePriority(priority);
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
	    	return effectivePriority;
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
			this.effectivePriority = priority; // should we reset or shift this value?
		}

		public void setEffectivePriority(int priority) {
			if (this.effectivePriority == priority)
				return;

			this.effectivePriority = priority;
		}

		/**
		 * <p>
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 * <p>
		 * Like its static counterpart, it iterates through the queue and
		 * places the thread in sorted order, but according to its effective
		 * priority rather than its initial priority.
		 *
		 * @param	pQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue pQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());

			ListIterator<KThread> iter = pQueue.waitQueue.listIterator();
			while ( iter.hasNext() ) { // insert at correct position
				ThreadState element = getThreadState(iter.next());
				if (element.getEffectivePriority() > this.effectivePriority) {
					pQueue.waitQueue.add( iter.previousIndex(), this.thread );
					//System.out.println(this.thread.toString() + " added to queue at position " + iter.previousIndex() + "; effective priority: " + effectivePriority);
					break;
				}
			}
			if (!iter.hasNext()) { // add to empty or end of list
				pQueue.waitQueue.add(this.thread);
				//System.out.println(this.thread.toString() + " added to end of queue at position " + (iter.previousIndex()+1) + "; effective priority: " + effectivePriority);
			}
		}

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
		public void acquire(PriorityQueue pQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(pQueue.waitQueue.isEmpty());
		}	

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the thread based on age */
		protected int effectivePriority;
		/** The total wait time of this thread */
		protected long waitTime = 0;
		/** The total run time of this thread */
		protected long runTime = 0;

		protected Date arrivalTime;
		protected Date lastUpdate;
		protected Date departureTime;

		public void updateRunTime() {
			Date newUpdate = new Date();
			runTime += newUpdate.getTime() - lastUpdate.getTime();
			lastUpdate = newUpdate;
		}

		public void updateWaitTime() {
			Date newUpdate = new Date();
			waitTime += newUpdate.getTime() - lastUpdate.getTime();
			lastUpdate = newUpdate;
		}

		public void setDepartureTime() {
			departureTime = new Date();
		}
    }
}
