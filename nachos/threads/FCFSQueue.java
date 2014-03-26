package nachos.threads;
import nachos.machine.*;
import java.util.LinkedList;


public class FCFSQueue extends ThreadQueue {
		FCFSQueue(){}
		
		

		public void waitForAccess(KThread thread) {
			waitQueue.add(thread);
		}

		public void acquire(KThread thread) {
			 Lib.assertTrue(Machine.interrupt().disabled());
		       
			 Lib.assertTrue(waitQueue.isEmpty());
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (waitQueue.isEmpty())
				return null;
			return waitQueue.removeFirst();
		}

		public void print(){}

		private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
	}



