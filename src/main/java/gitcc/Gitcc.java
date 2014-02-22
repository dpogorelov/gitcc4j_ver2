package gitcc;

import gitcc.cc.Clearcase;
import gitcc.cc.ClearcaseCmdImpl;
import gitcc.cc.ClearcaseRCImpl;
import gitcc.cc.UCM;
import gitcc.cmd.Checkin;
import gitcc.cmd.Command;
import gitcc.cmd.Daemon;
import gitcc.cmd.Rebase;
import gitcc.cmd.Reset;
import gitcc.config.Config;
import gitcc.config.ConfigParser;
import gitcc.git.Git;
import gitcc.git.GitImpl;
import gitcc.util.ExecException;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

@SuppressWarnings("unchecked")
public class Gitcc {
	private static Logger log = Logger.getLogger(Gitcc.class);
	
	private static final String GITCC4J_PATH = "./conf/gitcc4j";
	private static final String USERS_PATH = "./conf/users";
	private static final String LOG4J_PATH = "./conf/log4j.xml";
	
	private static Class<Command>[] commands = new Class[] { Checkin.class,
			Rebase.class, Reset.class, Daemon.class };

	public static void main(String[] args) throws Exception {
		try {
			DOMConfigurator.configure(LOG4J_PATH);
			ProxyHelper.initProxy();
			if (args.length == 0) {
				help();
			}
			String type = args[0];
			Command command = getCommand(type);
	
			Config config = command.config = new Config();
			String pathGit = args[1] != null ? args[1] : ".";
			Git git = command.git = new GitImpl(findGitRoot(pathGit));
			command.init();
			config.setBranch(git.getBranch());
	
			ConfigParser parser = new ConfigParser();
			if (!parser.parseConfig(config, new File(GITCC4J_PATH)))
				fail("Missing configuration file: " + GITCC4J_PATH);
			if (!parser.loadUsers(config, new File(USERS_PATH)))
				fail("Missing users file: " + USERS_PATH);
			
			if( config.getTempDir() == null )
				fail("You should set temporary directory for files from ClearCase");
			
			command.cc = createClearcase(command.config);
			command.execute();
			System.exit(0);
		} catch (ExecException e) {
			fail(e.getMessage());
		} catch (Exception e) {
			log.error(e);
			System.exit(1);
		}
	}

	private static void fail(String message) {
		log.error(message);
		System.exit(1);
	}

	private static File findGitRoot(String pathGit) throws Exception {
        File dir = new File(pathGit).getAbsoluteFile();
		while (!Arrays.asList(dir.list()).contains(".git")) {
			dir = dir.getParentFile();
			if (dir == null)
				throw new Exception("No git directory found");
		}
		return dir;
	}

	private static Clearcase createClearcase(Config config) throws Exception {
		if (config.isCMD()) {
            return new ClearcaseCmdImpl(config);
        } else if (config.isUCM()) {
			return new UCM(config);
		} else {
			return new ClearcaseRCImpl(config);
		}
	}

	private static Command getCommand(String type) throws Exception {
		for (Class<Command> c : commands) {
			if (c.getSimpleName().equalsIgnoreCase(type)) {
				return c.newInstance();
			}
		}
		help();
		return null;
	}

	private static void help() throws Exception {
		log.info("gitcc COMMAND [ARGS]");
		System.exit(1);
	}
}
