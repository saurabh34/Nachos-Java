package nachos.threads;
import nachos.machine.*;

public class SystemTime {
	
	
	public final static long baslineTime=System.currentTimeMillis();	
	static{
		System.out.println("Starting clock Time (Base refrence in milliseconds)= "+baslineTime);
	}
    
	
	public static void main(String[] args) {
		System.out.println("Starting time refrence= "+baslineTime);
	}

	public static long getTime(){
		return System.currentTimeMillis()-baslineTime;
	//	return Machine.timer().getTime();
	}
	
	public static long getBaseTime(){
		System.out.println("Starting clock Time (Base refrence in milliseconds)= "+baslineTime);
		return baslineTime;
	//	return Machine.timer().getTime();
	}
	
}
