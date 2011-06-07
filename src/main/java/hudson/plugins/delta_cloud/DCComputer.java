package hudson.plugins.delta_cloud;

import hudson.slaves.SlaveComputer;

import java.io.IOException;

import org.deltacloud.Instance;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

public class DCComputer extends SlaveComputer{

	private Instance instance;
	
	DCSlave slave;
	
	public DCComputer(DCSlave slave){
		super(slave);
		this.slave = slave;
	}
	
	public DCSlave getNode(){
		return slave;
	}
	
	public String getPubAddress(){
		return instance.getPublicAddresses().getAddresses().get(0);
	}
	
	/**
    * When the slave is deleted, terminate the instance.
    */
   @Override
   public HttpResponse doDoDelete() throws IOException {
       checkPermission(DELETE);
       getNode().terminate();
       return new HttpRedirect("..");
   }
}
