package hudson.plugins.delta_cloud;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deltacloud.HardwareProfile;
import org.deltacloud.Image;
import org.deltacloud.Realm;
import org.deltacloud.client.DeltaCloudClient;
import org.deltacloud.client.DeltaCloudClientException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SlaveImage implements Describable<SlaveImage>{
	
	private String realm;
	private String hwProfile;
	private String dcImage;
	
	//Slave config
	//private String slaveName;
	private String description;
	private int numExec;
	private String remoteFS;
	private String labels;
	
	//SSH launcher config
	private String username;
	private String password;
	private String keyName;
	private String privKey;
	private int port;
	private String jvmOptions;
	private String javaPath;
	
	
	
	@DataBoundConstructor
	public SlaveImage(String realm, String hwProfile, String dcImage, String description, String numExec, String remoteFS, String lables, String username, String password, String keyName, String privKey, String port, String jvmOptions, String javaPath){
		this.realm = realm;
		this.hwProfile = hwProfile;
		this.dcImage = dcImage;
	
		this.description = description;
		try{
			this.numExec = Integer.parseInt(numExec);
		}catch(NumberFormatException e){
			this.numExec = 1;
		}
		this.remoteFS = remoteFS;
		this.labels = lables;
		
		this.username = username;
		this.password = password;
		this.keyName = keyName;
		this.privKey = privKey;
		try{
			this.port = Integer.parseInt(port);
		}catch(NumberFormatException e){
			this.port = 22;
		}
		this.jvmOptions = jvmOptions;
		this.javaPath = javaPath;
	}

	public String getRealm() {
		return realm;
	}
	
	public String getHwProfile(){
		return hwProfile;
	}

	public String getDcImage() {
		return dcImage;
	}

	public String getDescription() {
		return description;
	}

	public int getNumExec() {
		return numExec;
	}

	public String getRemoteFS() {
		return remoteFS;
	}

	public String getLabels() {
		return labels;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getKeyName(){
		return keyName;
	}
	
	public String getPrivKey() {
		return privKey;
	}

	public int getPort() {
		return port;
	}

	public String getJvmOptions() {
		return jvmOptions;
	}

	public String getJavaPath() {
		return javaPath;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

	public Descriptor<SlaveImage> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }
	
	@Extension
    public static final class DescriptorImpl extends Descriptor<SlaveImage> {
        
		public static DeltaCloudClient client;
		public static List<Realm> realms;
		public static List<HardwareProfile> hwProfiles;
		public static List<Image> images;
		public static boolean isLoaded;
		
		public String getDisplayName() {
            return null;
        }
        
        public ListBoxModel doFillRealmItems(
        		//TODO otestovat QueryParameter("dc.apiUrl")
        		@QueryParameter(value="dc.apiUrl") @RelativePath("..") String apiUrl, 
        		@QueryParameter(value="dc.login") @RelativePath("..") String login,
				@QueryParameter(value="dc.passwd") @RelativePath("..") String passwd) {
        	System.out.println("login/passwd:" + apiUrl + " " + login + " " + passwd);
			ListBoxModel model = new ListBoxModel();
			if(!isLoaded) {
				loadApi(apiUrl, login, passwd);
			}
			for (Realm reaml : realms) {
				model.add(reaml.getName());
			}
			return model;
		}
        
        public ListBoxModel doFillHwProfileItems(
        		//TODO otestovat QueryParameter("dc.apiUrl")
        		@QueryParameter(value="dc.apiUrl") @RelativePath("..") String apiUrl, 
        		@QueryParameter(value="dc.login") @RelativePath("..") String login,
				@QueryParameter(value="dc.passwd") @RelativePath("..") String passwd) {
			ListBoxModel model = new ListBoxModel();
			if(!isLoaded) {
				loadApi(apiUrl, login, passwd);
			}
			for (HardwareProfile profile : hwProfiles) {
				model.add(profile.getName());
			}
			return model;
		}
        
		public ListBoxModel doFillDcImageItems(
				@QueryParameter(value="dc.apiUrl") @RelativePath("..") String apiUrl, 
				@QueryParameter(value="dc.login") @RelativePath("..") String login,
				@QueryParameter(value="dc.passwd") @RelativePath("..") String passwd) {
			System.out.println("skutecne v dcImageItems");
			System.out.println("api/login/passwd:" + apiUrl + " " + login + " " + passwd);
			ListBoxModel model = new ListBoxModel();
			if (isLoaded) {
				loadApi(apiUrl, login, passwd);
			}
			System.out.println("Filling images: " + images);
			for (Image image : images) {
				model.add(image.getName(), image.getId());
			}
			return model;
		}
		
		private FormValidation loadApi(String apiUrl, String login, String passwd) {
			try {
				URL deltaCloudURL = new URL(apiUrl);
				client = new DeltaCloudClient(deltaCloudURL, login, passwd);
				realms = client.listRealms();
				hwProfiles = client.listHardwareProfiles();
				images = client.listImages();
				System.out.println("realms: " + realms);
				System.out.println("hw profiles: " + hwProfiles);
				System.out.println("images: " + images);
				isLoaded = true;
			} catch (MalformedURLException e) {
				LOGGER.log(Level.WARNING, "Fails to create Delta cloud client, malformaed URL", e);
				e.printStackTrace();
				return FormValidation.error(e.getMessage());
			} catch (DeltaCloudClientException e) {
				LOGGER.log(Level.WARNING, "Failed to create Delta cloud client", e);
				return FormValidation.error(e.getMessage());
			}
			return FormValidation.ok("Succefully loaded");
		}
        
	}
	
	private static final Logger LOGGER = Logger.getLogger(DeltaCloud.class.getName());
}
