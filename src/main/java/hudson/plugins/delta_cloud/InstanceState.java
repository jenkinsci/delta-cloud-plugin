package hudson.plugins.delta_cloud;

public enum InstanceState {
	PENDING,
	RUNNING,
	STOPPED;
	
	public static InstanceState getState(String state){
		return InstanceState.valueOf(state);
	}
	
	public static boolean isRunning(String state){
		if(RUNNING.name().equals(state))
			return true;
		return false;
	}
}
