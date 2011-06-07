package hudson.plugins.delta_cloud;

import hudson.slaves.RetentionStrategy;
import hudson.util.TimeUnit2;

import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;


public class DCRetentionStrategy extends RetentionStrategy<DCComputer>{

	@DataBoundConstructor
    public DCRetentionStrategy() {
    }

	public synchronized long check(DCComputer c) {
        if (c.isIdle()) {
            final long idleMilliseconds = System.currentTimeMillis() - c.getIdleStartMilliseconds();
            if (idleMilliseconds > TimeUnit2.MINUTES.toMillis(20)) {
                LOGGER.info("Disconnecting "+c.getName());
                c.getNode().terminate();
            }
        }
        return 1;
    }

	@Override
    public void start(DCComputer c) {
        c.connect(false);
    }
	
	private static final Logger LOGGER = Logger.getLogger(DCRetentionStrategy.class.getName());
	
}