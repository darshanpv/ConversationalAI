package cto.hmi.processor;

import java.util.Properties;

/**
 * Store configuration properties (like paths etc) here
 *
 */
public class ConvEngineConfig extends Properties{

	private static final long serialVersionUID = 1L;
	private static ConvEngineConfig instance=null;
	public static final String DIALOGUEDIR="DialogueDir";
	public static final String CONFIGFILE="ConfigFile";
	public static final String BROKERFILE="BrokerFile";
	public static final String ROLESFILE="RolesFile";
	public static final String RESOURCEBUNDLEFOLDER="ResourceBundleFolder";
	public static final String JETTYKEYSTOREPATH="JettyKeyStorePath";
	public static final String JETTYKEYSTOREPASS="JettyKeyStorePass";
	public static final String NLPKEYSTOREPATH="NLPKeyStorePath";
	public static final String NLPKEYSTOREPASS="NLPKeyStorePass";
	public static final String JETTYWEBXMLPATH="WebXml";
	public static final String JETTYRESOURCEBASE="ResourceBase";
	public static final String JETTYCONTEXTPATH="ContextPath";
	
	private String basedir = "file:///"+System.getProperty("user.dir");
	
	private ConvEngineConfig(){

	}
	
	public static ConvEngineConfig getInstance(){
		if(instance==null){
			instance=new ConvEngineConfig();
			instance.init();
		}
		return instance;
	}
	
	public void setBaseDir(String path){
		basedir=path;
		init();
	}
	
	private void init(){
		this.setProperty(DIALOGUEDIR, basedir+"/res/dialogues");
		this.setProperty(CONFIGFILE, basedir+"/res/config/bot.properties");
		this.setProperty(BROKERFILE, basedir+"/res/config/broker.properties");
		this.setProperty(ROLESFILE, basedir+"/res/config/roles.properties");
		this.setProperty(RESOURCEBUNDLEFOLDER, basedir+"/res/config");
		//Jetty SSL
		this.setProperty(JETTYKEYSTOREPATH, "res/keys/hmi.jks");
		this.setProperty(JETTYKEYSTOREPASS, "naturaldialog");
		
		//NLP SSL
		this.setProperty(NLPKEYSTOREPATH, "res/keys/nlp.jks");
		this.setProperty(NLPKEYSTOREPASS, "naturaldialog");
		
		//Jetty
		this.setProperty(JETTYWEBXMLPATH, "./WEB-INF/web.xml");
		this.setProperty(JETTYRESOURCEBASE, "res/html");
		this.setProperty(JETTYCONTEXTPATH, "/");
	}

}
