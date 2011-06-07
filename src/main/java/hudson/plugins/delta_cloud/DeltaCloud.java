package hudson.plugins.delta_cloud;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Node.Mode;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deltacloud.Instance;
import org.deltacloud.Realm;
import org.deltacloud.client.DeltaCloudClient;
import org.deltacloud.client.DeltaCloudClientException;
import org.kohsuke.stapler.DataBoundConstructor;

public class DeltaCloud extends Cloud {

	private final String apiUrl;
	private final String login;
	private final String passwd;
	// private Realm realm;

	private transient DeltaCloudClient client;

	private transient SecureRandom rand;

	private List<SlaveImage> images;

	// private final SlaveImage image;

	@DataBoundConstructor
	public DeltaCloud(String apiUrl, String login, String passwd, List<SlaveImage> images) {
		super("Delta cloud");
		this.apiUrl = apiUrl;
		this.login = login;
		this.passwd = passwd;
		this.images = images;
		LOGGER.finer("image:" + images);

		client = initClient();
		rand = new SecureRandom();
		/*
		 * try{ this.realm = client.listRealms(realmId); } catch(DeltaCloudClientException e){ e.printStackTrace(); }
		 */
	}

	protected Object readResolve() {
		rand = new SecureRandom();
		client = initClient();
		return this;
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public String getLogin() {
		return login;
	}

	public String getPasswd() {
		return passwd;
	}

	public List<SlaveImage> getImages() {
		return images;
	}

	/*
	 * public Realm getRealm() { return realm; }
	 */
	public DeltaCloudClient getClient() {
		return client;
	}

	public List<Realm> getRealms() throws DeltaCloudClientException {
		return client.listRealms();
	}

	public boolean canProvision(Label label) {
		return true;
	}

	public Collection<PlannedNode> provision(Label label, int excessWorkload) {
		Collection<PlannedNode> nodes = new ArrayList<PlannedNode>();
		while (excessWorkload > 0) {
			LOGGER.info("Excess workload: " + excessWorkload);
			LOGGER.info("Provisioning new DC machine");
			Future<Node> node = Computer.threadPoolForRemoting.submit(new Launcher(images.get(0)));
			nodes.add(new PlannedNode(name, node, 1));
			excessWorkload -= 1;
		}
		return nodes;
	}

	private final class Launcher implements Callable<Node> {

		private final SlaveImage image;
		private DeltaCloudClient client;

		public Launcher(SlaveImage image) {
			this.image = image;
			client = initClient();
		}

		public Node call() throws Exception {
			LOGGER.info("Starting DC slave with image " + image.getDcImage());
			System.out.println("client: " + client);
			System.out.println("image: " + image);
			System.out.println("rand: " + rand);
			Instance instance = client.createInstance(image.getDcImage(), image.getHwProfile(), image.getRealm(), "DC-"
					+ image.getDcImage() + rand.nextInt(),image.getKeyName());
			while (!InstanceState.isRunning(client.listInstances(instance.getId()).getState())) {
				LOGGER.log(Level.FINEST,"instance id: " + instance.getId() + ", state:" + instance.getState());
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//make sure that the instance is really only, EC2 reports the instance to be running, but it often happened that
			//it refuses ssh connection and results into failed launch of slave and slave has to be put online manually later on
			try {
				Thread.sleep(120000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//refresh instance to have public address properly loaded
			instance = client.listInstances(instance.getId());
			LOGGER.info("Machine ready, address: " + instance.getPublicAddresses().toString());
			System.out.println("Machine ready, address: " + instance.getPublicAddresses().toString());
			System.out.println("Machine ready, address: " + instance.getPublicAddresses().getAddresses().get(0));
			SSHLauncher launcher = new SSHLauncher(instance.getPublicAddresses().getAddresses().get(0),image.getPort(), image.getUsername(), image.getPassword(),
					image.getPrivKey(), image.getJvmOptions(), image.getJavaPath());
			DCRetentionStrategy strategy = new DCRetentionStrategy();
			DCSlave slave = new DCSlave(instance, client, "DC-" + instance.getId(), "Delta cloud slave "
					+ instance.getName(), image.getRemoteFS(), image.getNumExec(), Mode.NORMAL, "DC", launcher, strategy, new ArrayList());
			Hudson.getInstance().addNode(slave);
			LOGGER.log(Level.FINER,"Slave ready");
			return slave;
		}
	}

	private DeltaCloudClient initClient() {
		DeltaCloudClient client = null;
		try {
			URL deltaCloudURL = new URL(apiUrl);
			client = new DeltaCloudClient(deltaCloudURL, login, passwd);
		} catch (MalformedURLException e) {
			LOGGER.log(Level.WARNING, "Delta cloud API wrong URL", e);
		} catch (DeltaCloudClientException e) {
			LOGGER.log(Level.WARNING, "Delta cloud init error", e);
		}
		return client;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<Cloud> {
		@Override
		public String getDisplayName() {
			return "Delta Cloud API";
		}
	}

	private static final Logger LOGGER = Logger.getLogger(DeltaCloud.class.getName());

}
