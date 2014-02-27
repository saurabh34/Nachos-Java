package nachos.threads;

import nachos.machine.*;


/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	Lib.assertTrue(currentThread != null);
	return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
	if (currentThread != null) {
	    tcb = new TCB();
	}	    
	else {
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	    readyQueue.acquire(this);	    

	    currentThread = this;
	    tcb = TCB.currentTCB();
	    name = "main";
	    restoreState();

	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	boolean intStatus = Machine.interrupt().disable();

	tcb.start(new Runnable() {
		public void run() {
		    runThread();
		}
	    });

	ready();
	
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	target.run();
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	Machine.interrupt().disable();

	Machine.autoGrader().finishingCurrentThread();

	Lib.assertTrue(toBeDestroyed == null);
	toBeDestroyed = currentThread;


	currentThread.status = statusFinished;
	
	long currentRunTime=Machine.timer().getTime()-currentThread.startRunTime;
	currentThread.soFarRunTime=currentThread.soFarRunTime+currentRunTime;
	currentThread.DepartureTime=Machine.timer().getTime();
	DynamicPriorityScheduler.printLog(currentThread.getName()+","+currentThread.arrivalTime+","+currentThread.soFarRunTime+","+currentThread.soFarWaitTime+","+currentThread.DepartureTime);
	sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assertTrue(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();

	currentThread.ready();

	runNextThread();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;
	
	
	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	if (this != idleThread)
	    readyQueue.waitForAccess(this);
	
	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());

	Lib.assertTrue(this != currentThread);

    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	if (nextThread == null)
	    nextThread = idleThread;

	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	currentThread.startRunTime=Machine.timer().getTime();
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());

	Machine.autoGrader().runningThread(this);
	
	status = statusRunning;
    
	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		currentThread.yield();
	    }
	}

	private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest()
    {
        // Test 1 - We have 3 threads, low, med and high
        // high runs and waits for med to release a sema4
        // once the sema4 is released, when both high and low are
        // in the ready state, high will run before low
      //  Semaphore s     = new Semaphore(0);
    
    	KThread low     = new KThread(new LowPriorityThread()).setName("Low1");
        KThread med     = new KThread(new MediumPriorityThread()).setName("Med1");
        KThread high    = new KThread(new HighPriorityThread()).setName("High1");
       
       
       
        boolean oldInterrupStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(low, 3);
        ThreadedKernel.scheduler.setPriority(med, 1);
        ThreadedKernel.scheduler.setPriority(high, 1);
        Machine.interrupt().restore(oldInterrupStatus);

        high.fork();
        med.fork();
        low.fork();
     //  low.yield();
      //  currentThread.yield();
      //low.join();
    
       
        // Test 2 - priority inversion
        // We have 3 threads t1 t2 t3 with priorities 1, 2, 3 respectively
        // t3 waits on a sema4, then t1 	 running wakes t2 then yields
        // now t2 is running and if t1 doesn't get a donation it won't release t3
      /*
        Semaphore s1    = new Semaphore(0);
        KThread t1      = new KThread(new T1(s1)).setName("T1");
        KThread t2      = new KThread(new T2()).setName("T2");
        KThread t3      = new KThread(new T3(s1)).setName("T3");
       
        oldInterrupStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(t3, 1);
        ThreadedKernel.scheduler.setPriority(t2, 2);
        ThreadedKernel.scheduler.setPriority(t1, 3);
        Machine.interrupt().restore(oldInterrupStatus);
               
                t1.fork();
                t3.fork();
                t2.fork();
               
                t1.join();
                t2.join();
                t3.join();
       
        */
        currentThread.yield();
    
    }

    
    private static final char dbgThread = 't';
    
    private static class HighPriorityThread implements Runnable
    {
       // HighPriorityThread(Semaphore sema4)
        //{
               // this.sema4 = sema4;
        //}
        
    	
    	public void run()
        {     
    		for(int j=0;j<5;j++){
                for(int i = 0; i < 60; ++i)
                {
                      Lib.debug(dbgThread, "$$$ HighPriorityThread running, i = " + i);
                      
                }
          //     boolean oldInterrupStatus = Machine.interrupt().disable();
               KThread.currentThread.yield();
             //  Machine.interrupt().restore(oldInterrupStatus);
               
             
          
        }
                // wait for someone else to release the sema4
              //  Lib.debug(dbgThread, "$$$ HighPriorityThread - before Semaphore.P()");
               // sema4.P();
               // Lib.debug(dbgThread, "$$$ HighPriorityThread - after Semaphore.P()");
        }
       // private Semaphore sema4;
    }
   
    private static class MediumPriorityThread implements Runnable
    {
        //MediumPriorityThread(Semaphore sema4)
        //{
         //       this.sema4 = sema4;
        //}
        public void run()
        {
                // release the sema4
              //  Lib.debug(dbgThread, "$$$ MediumPriorityThread before Semaphore.V()");
          //      sema4.V();
               // Lib.debug(dbgThread, "$$$ MediumPriorityThread after Semaphore.V()");
            for (int j=0;j<10;j++) {   
        	for(int i = 0; i < 10; ++i)
                {
                        Lib.debug(dbgThread, "$$$ MediumPriorityThread running, i = " + i);
                }
            KThread.currentThread.yield();
            }
         }
        //private Semaphore sema4;
    }
   
    private static class LowPriorityThread implements Runnable
    {
        LowPriorityThread()
        {
        }
        public void run()
        {
                for(int i = 0; i < 20; ++i)
                {
                        Lib.debug(dbgThread, "$$$ LowPriorityThread running, i = " + i);
                }
        }
    }
   
    private static class T1 implements Runnable
    {
        T1(Semaphore sema4)
        {
                this.sema4 = sema4;
        }
        public void run()
        {
                Lib.debug(dbgThread, "&&& T1 starting");
                for(int i = 0; i < 10; ++i)
                {
                        Lib.debug(dbgThread, "&&& T1 running, i = " + i);
                }
                Lib.debug(dbgThread, "&&& T1 - Yielding, to make sure I get preempted by T2");
                KThread.currentThread().yield();
                Lib.debug(dbgThread, "&&& T1 - before Semaphore.V()");
                sema4.V();
                Lib.debug(dbgThread, "&&& T1 - after Semaphore.V()");
        }
        private Semaphore sema4;
    }
   
    private static class T2 implements Runnable
    {
        T2()
        {
        }
        public void run()
        {
                KThread.currentThread().yield();
                for(int i = 0; i < 100; ++i)
                {
                        Lib.debug(dbgThread, "&&& T2 running, i = " + i);
                        if(i == 50)
                        {
                                Lib.debug(dbgThread, "&&& T2 yelding");
                                KThread.currentThread().yield();
                        }
                }
                Lib.debug(dbgThread, "&&& T2 before yelding");
                KThread.currentThread().yield();
                Lib.debug(dbgThread, "&&& T2 after yelding");
                for(int i = 100; i < 120; ++i)
                {
                        Lib.debug(dbgThread, "&&& T2 running, i = " + i);
                }
               
        }
    }
   
    private static class T3 implements Runnable
    {
        T3(Semaphore sema4)
        {
                this.sema4 = sema4;
        }
        public void run()
        {
                Lib.debug(dbgThread, "&&& T3 before P()");
                sema4.P();
                Lib.debug(dbgThread, "&&& T3 after P()");
                for(int i = 0; i < 20; ++i)
                {
                        Lib.debug(dbgThread, "&&& T3 running, i = " + i);
                }
        }
        private Semaphore sema4;
    }


    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;
  //  public long arrivalTime=0;
    public long DepartureTime=0;
    public long soFarWaitTime=0;
    public long soFarRunTime=0;
    public long startWaitTime=0;
    public long startRunTime=0;
    public long arrivalTime=0;
   
    
    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;
    
   
    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;
 
    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
