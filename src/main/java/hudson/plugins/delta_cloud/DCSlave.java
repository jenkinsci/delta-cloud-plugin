package hudson.plugins.delta_cloud;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deltacloud.Instance;
import org.deltacloud.client.DeltaCloudClient;
import org.deltacloud.client.DeltaCloudClientException;
import org.kohsuke.stapler.DataBoundConstructor;

public class DCSlave extends Slave implements EphemeralNode {

	//public final String initScript;
	private final Instance instance;
	private final DeltaCloudClient client;

	@DataBoundConstructor
	public DCSlave(Instance instance, DeltaCloudClient client, String name, String nodeDescription, String remoteFS, int numExecutors, Mode mode,
			String labelString, ComputerLauncher launcher, RetentionStrategy retentionStrategy,
			List<? extends NodeProperty<?>> nodeProperties) throws FormException, IOException {
		super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy,
				nodeProperties);
		//this.initScript = initScript;
		this.instance = instance;
		this.client = client;
	}

	/**
	 * Terminates the instance.
	 */
	public void terminate() {
		try {
			LOGGER.info("Terminated Dela cloud slave instance: ");
			Hudson.getInstance().removeNode(this);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to terminate Delta cloud slave instance: ", e);
		}
		// stop and remove instances
		// TODO instance pool, just stop instance and reuse
		try {
			LOGGER.info("Stoping DC instance");
			client.performInstanceAction(instance.getId(), "stop");
			LOGGER.info("Destroying DC instance");
			client.performInstanceAction(instance.getId(), "destroy");
		} catch (DeltaCloudClientException e) {
			LOGGER.log(Level.WARNING,"Cannot stop or remove delta cloud instance!",e);
		}
	}

	public Node asNode() {
		return this;
	}

	@Override
	public Computer createComputer() {
		return new DCComputer(this);
	}

	@Extension
	public static final class DescriptorImpl extends SlaveDescriptor {
		public String getDisplayName() {
			return "Delta Cloud API";
		}

		@Override
		public boolean isInstantiable() {
			return false;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(DCSlave.class.getName());

}
