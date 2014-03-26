package nachos.threads;

import nachos.machine.Machine;


public class LockTest {

	private static class AcessSharedData implements Runnable {
		AcessSharedData() { }

		public void run() {
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock and entering critical section");
			sharedData=sharedData+1;
			System.out.println("shared data value: "+sharedData);
			KThread.currentThread().yield();
			lock.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock and exiting critical section");
		}
	}

	public static void simpleTest() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread t1 = new KThread(new AcessSharedData()).setName("t1");
		KThread t2 = new KThread(new AcessSharedData()).setName("t2");
		KThread t3 = new KThread(new AcessSharedData()).setName("t3");
		KThread t4 = new KThread(new AcessSharedData()).setName("t4");
		

	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(t1, 32);
	    ThreadedKernel.scheduler.setPriority(t2, 10);
	    ThreadedKernel.scheduler.setPriority(t3, 2);
	    ThreadedKernel.scheduler.setPriority(t4, 2);
	    Machine.interrupt().restore(oldInterrupStatus);
		t1.fork();
		KThread.yield();
		t2.fork();
	    t3.fork();
	    t4.fork();
	    //KThread.yield();
	     
	    //t4.join();
		

	}

	static Lock lock = new Lock();
	static int sharedData = 0;


}
