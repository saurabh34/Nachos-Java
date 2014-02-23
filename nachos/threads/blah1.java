package nachos.threads;

import nachos.machine.*;

import java.util.TreeMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.LinkedList;

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
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
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
     * @param   transferPriority        <tt>true</tt> if this queue should
     *                                  transfer priority from waiting threads
     *                                  to the owning thread.
     * @return  a new priority thread queue.
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
        Lib.assertTrue(Machine.interrupt().disabled());
                       
        Lib.assertTrue(priority >= getPriorityMin() &&
                   priority <= getPriorityMax());
       
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
                       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == getPriorityMax())
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
                       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == getPriorityMin())
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
   
    protected int getPriorityDefault()
    {
        return priorityDefault;
    }
    protected int getPriorityMin()
    {
        return priorityMinimum;
    }
    protected int getPriorityMax()
    {
        return priorityMaximum;
    }
   
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param   thread  the thread whose scheduling state to return.
     * @return  the scheduling state of the specified thread.
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
            getThreadState(thread).acquire(this);
        }

        //Q5 this method is different from the pickNextThread
        //since it will modify the state of the threads waiting
        //in the data structure PQueue.
        public KThread nextThread() {
                //runningThread is the thread
                //that's hold the current lock.
                //since it has finished, the nextThread will get the
                //lock and will start to run
                if(runningThread != null)
                {
                     runningThread = null;
                }
            Lib.assertTrue(Machine.interrupt().disabled());
           
            ThreadState nThread = pickNextThread(); //returns the highest priority threadState in the pQueue 
            if(nThread != null)               //and among same priority,returns the max time waiting thread
            {   
                Integer highP = pQueue.lastKey();
                LinkedList<ThreadState> threads = pQueue.get(highP);
                threads.remove(nThread); //not the index but removes that thread state.
                if(threads.size() == 0)
                {
                        pQueue.remove(highP);
                }
                nThread.acquire(this); //acquire the resource
                return nThread.thread;
            }
           
            return null;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return      the next thread that <tt>nextThread()</tt> would
         *              return.
         */
        protected ThreadState pickNextThread() {
            if(!pQueue.isEmpty())              
            {
               
                ThreadState retValue = null;
                Integer highP = pQueue.lastKey();
                LinkedList<ThreadState> threads = pQueue.get(highP);
               
                //there are multiple threads waiting for the given priority
                if(threads.size() > 1)
                {
                        long prevWaitTime = 0;
                        for(int i = 0; i < threads.size() ; i++)
                        {
                                long currWaitTime = Machine.timer().getTime() - threads.get(i).insertTime;
                                if(currWaitTime > prevWaitTime)
                                {
                                        prevWaitTime = currWaitTime;
                                        retValue = threads.get(i);
                                }
                        }
                }
                else
                {
                        //there is only one threads waiting for the given priority
                        retValue = threads.get(0);
                }
               
                return retValue;
            }
            return null;
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
       
        //Q5
        //This list maintains the threads in the queue that are
        //wait for the resource.
        TreeMap<Integer,LinkedList<ThreadState>> pQueue = new TreeMap<Integer,LinkedList<ThreadState>>();
        public ThreadState runningThread;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see     nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param       thread  the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
           
            setPriority(getPriorityDefault());
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return      the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return      the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
          
            return priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param       priority        the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;
           
            if(priority <= getPriorityMin())
            {
                this.priority = getPriorityMin();
            }
            else if(priority >= getPriorityMax())
            {
                this.priority = getPriorityMax();
            }
            else
            {
                this.priority = priority;
            }
           
           
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param       waitQueue       the queue that the associated thread is
         *                              now waiting on.
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
                
        	    Integer currPriority = new Integer(this.getEffectivePriority());
                LinkedList<ThreadState> waitingTh = waitQueue.pQueue.get(currPriority);
                if(waitingTh == null)
                {
                        waitingTh = new LinkedList<ThreadState>();
                        waitQueue.pQueue.put(currPriority,waitingTh);
                }
                this.insertTime = Machine.timer().getTime();
            waitingTh.add(this);
           
            
        }

        /**
         * Called when the associated thread has acquired access to whatever resources (CPU...) is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            if(waitQueue.transferPriority)
            {
                waitQueue.runningThread = this;
            }
        }      

        /** The thread with which this object is associated. */    
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        public int oldPriority;
        public boolean inheritedPriority = true;
        public long insertTime;
       // public TreeMap<Integer,LinkedList<ThreadState>> listOfHPThreads = new TreeMap<Integer,LinkedList<ThreadState>>();
    }
   
               
}   