package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */
public class Lock {
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     */
    public Lock() {
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     */
    public void acquire() {
	Lib.assertTrue(!isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();
	KThread thread = KThread.currentThread();

	if (lockHolder != null) {
		// propogating priority from thread to lockHolder
		System.out.println("propogating priority");
		thread.WaitingOnlockThread=lockHolder;
		propogatePriority(thread, lockHolder);
		
		System.out.println("Reordering the Scheduler Priority Queue");
		KThread.reorderSchedulerPriorityQueue();
		
		waitQueue.waitForAccess(thread);
	    System.out.println("Sending thread "+KThread.currentThread().getName()+" to block/sleep state and put on lock waitQueue");
	    KThread.sleep();
	}
	else {
	    waitQueue.acquire(thread);
	    lockHolder = thread;
	}

	Lib.assertTrue(lockHolder == thread);

	Machine.interrupt().restore(intStatus);
    }
    
    //take care of two cases recursion should be inside if condition 2) what if from recieve thread changes 
    public void propogatePriority(KThread waitThread, KThread lockHolder) {
    	
    	if(lockHolder.inherentPriority > waitThread.inherentPriority){
	    	lockHolder.inherentPriority=waitThread.inherentPriority;
	    	System.out.println(lockHolder.getName()+" inheriting priority from "+ waitThread.getName());
	    	//waitThread.donatedPriorityThread = lockHolder;
	    	//waitThread.WaitingOnlockThread = lockHolder;
	     //   if (lockHolder.ReceivedPriorityThread !=null){
	     //   	lockHolder.ReceivedPriorityThread.donatedPriorityThread=null;
	      //  }
	      //  lockHolder.ReceivedPriorityThread = waitThread;
	       // KThread donatedThread = lockHolder.donatedPriorityThread;
	       
	        // propogating priority from lockHolder to donatedThread
	        if (lockHolder.WaitingOnlockThread!= null){
	        	//if (donatedThread.WaitingOnlockThread !=null){
	        		
	        	//}
	        	propogatePriority(lockHolder,lockHolder.WaitingOnlockThread);
	        }
	        else
	        	return;
	    }
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     */
    public void release() {
    	Lib.assertTrue(isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();
		
	//give access to next immediate thread in lock wait queue  and put in CPU scheduler ready queue
		//lockHolder.inherentPriority = lockHolder.originalPriority;
		ThreadedKernel.scheduler.setPriority(lockHolder, lockHolder.originalPriority);
		System.out.println("Restoring original priority of "+ lockHolder.getName()+ " to "+ lockHolder.originalPriority );
		System.out.println("Reordering the Scheduler Priority Queue");
		KThread.reorderSchedulerPriorityQueue();
		
		if ((lockHolder = waitQueue.nextThread()) != null){
			
			//revoke the lockHolder's priority and set the children thread of the parent thread to null
			lockHolder.WaitingOnlockThread=null;
			//KThread parent = lockHolder.ReceivedPriorityThread;
			//if(parent!=null) parent.donatedPriorityThread = null;  
			System.out.println("Waking up lock thread: "+lockHolder.getName());
			lockHolder.ready();                            
		}
		
		Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     *
     * @return	true if the current thread holds this lock.
     */
    public boolean isHeldByCurrentThread() {
	return (lockHolder == KThread.currentThread());
    }

    private KThread lockHolder = null;
    private ThreadQueue waitQueue =	new FCFSQueue();
    private String LockName = null;
}
