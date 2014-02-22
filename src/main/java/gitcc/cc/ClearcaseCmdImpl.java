package gitcc.cc;

import gitcc.config.Config;
import gitcc.config.User;
import gitcc.exec.Exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * @author d.pogorelov
 */
public class ClearcaseCmdImpl extends Exec implements Clearcase {
	private static Logger log = Logger.getLogger(ClearcaseCmdImpl.class);
	
    private static final String LSH_FORMAT = "%o%m|%Nd|%u|%En|%Vn|";
    private static final CCHistoryParser histParser = new CCHistoryParser();
    private static final String DATE_FORMAT = "dd-MMM-yyyy.HH:mm:ss";
    private static final String CLEAR_CASE_TOOL = "cleartool";

    private String extraPath;
    private File[] files;
    protected Config config;
    
    private DirDiffUtil diffUtil = new DirDiffUtil();

    public ClearcaseCmdImpl(Config config) {
        super(CLEAR_CASE_TOOL);
        this.config = config;
        setRoot(new File(config.getClearcase()));
    }

    public void setRoot(File root) {
        super.setRoot(root);
        extraPath = getRoot().getAbsolutePath() + File.separatorChar;
        String[] includes = config.getInclude();
        files = new File[includes.length];
        for (int i = 0; i < includes.length; i++) {
            files[i] = new File(root, includes[i]);
        }
    }

    @Override
    public Collection<CCCommit> getHistory(Date since) {
        String format = LSH_FORMAT + getCommentFormat() + CCHistoryParser.SEP;
        List<String> args = new ArrayList<String>();
        args.add("lshistory");
        args.add("-r");
        args.add("-fmt");
        args.add(format);
        if (since != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(since);
            c.add(Calendar.SECOND, 1);
            since = c.getTime();
            args.add("-since");
            args.add(new SimpleDateFormat(DATE_FORMAT).format(since));
        }
        for (File include : files) {
            args.add(include.getName());
        }
        
        String lsh = "";
        if( config.getHistory() == null ) {
        	lsh = exec(args);
        } else {
        	try {
				lsh = readFileAsString(config.getHistory());
			} catch (IOException e) {
				log.error(e.getMessage());
			}
        }
        
        Collection<CCCommit> commits = histParser.parse(lsh, config
                .getBranches());
        for (CCCommit commit : commits) {
            for (CCFile f : commit.getFiles()) {
            	if (f.getFile().startsWith(extraPath)) {
            		f.setFile(f.getFile().substring(extraPath.length()));
            	}
            }
            commit.setMessage(getRealComment(commit.getMessage()));
        }
        return commits;
    }
    
    private String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    protected String getRealComment(String comment) {
        return comment;
    }

    protected String getCommentFormat() {
        return "%Nc";
    }

    @Override
    public void update() {
    	// Do nothing for dynamic view
    	if( config.isStaticView() ) {
    		List<String> args = new ArrayList<String>();
            args.add("update");            
            exec(args);
    	}
    }

    @Override
    public void rebase() {
    	// Do nothing
    }

    @Override
    public void uncheckout(String f) {
    	List<String> args = new ArrayList<String>();
        args.add("unco");
        args.add("-rm");
        args.add(f);
    	
        exec(args);
    }

    @Override
    public void checkout(String file) {
    	List<String> args = new ArrayList<String>();
        args.add("co");
        args.add("-reserved");
        args.add("-nc");
        args.add(file);
    	
        exec(args);
    }

    @Override
    public void checkin(String file, String message) {
    	List<String> args = new ArrayList<String>();
        args.add("ci");
        args.add("-identical");
        args.add("-c");
        args.add(message);
        args.add(file);
    	
        exec(args);
    }

    @Override
    public void deliver() {
    	// Do nothing
    }

    @Override
    public String mkact(String message) {
    	return message;
    }

    @Override
    public void rmact(String activity) {
    	// Ignore
    }

    @Override
    public void move(String file, String newFile) {
    	List<String> args = new ArrayList<String>();
        args.add("mv");
        args.add("-nc");
        args.add(file);
        args.add(newFile);
    	
        exec(args);
    }

    @Override
    public void delete(String file) {
    	List<String> args = new ArrayList<String>();
        args.add("rm");
        args.add(file);
    	
        exec(args);
    }

    @Override
    public void add(String file, String message) {
    	List<String> args = new ArrayList<String>();
        args.add("mkelem");
        args.add("-nc");
        args.add(file);
    	
        exec(args);
    }

    @Override
    public void mkdir(String dir) {
    	copyFile(dir).mkdirs();
    	
    	List<String> args = new ArrayList<String>();
        args.add("mkelem");
        args.add("-nc");
        args.add("-eltype");
        args.add("directory");
        args.add(dir);
    	
        exec(args);
    }

    @Override
    public List<CCFile> diffPred(CCFile file) {
    	List<String> args = new ArrayList<String>();
        args.add("diff");
        args.add("-diff_format");
        args.add("-pred");
        args.add(file.toString());
    	
        String diff = exec(false, args);
        log.debug(diff);
        Set<String> deletedFiles = new HashSet<String>();
        Set<String> addedFiles = new HashSet<String>();
        for( String line : diff.split("\n") ) {
        	if( line.contains(" -> ") ) {
        		log.info("SYMLINK " + line);
        		continue;
        	}
        	
        	if( line.startsWith("<") ) {
        		deletedFiles.add(getPathFromDiff(line));
        	} else if( line.startsWith(">") ) {
        		addedFiles.add(getPathFromDiff(line));
        	}
        }
        
        return diffUtil.diff(file.getFile(), deletedFiles, addedFiles);
    }
    
    private String getPathFromDiff(String line) {
    	String[] splitted = line.split("\\s");
    	return splitted[1];
    }

    @Override
    public File get(CCFile f) {
    	String tempFile = config.getTempDir() + "temp.file";
    	List<String> args = new ArrayList<String>();
        args.add("get");
        args.add("-to");
        args.add(tempFile);
        args.add(f.toString());
    	
        exec(args);
    	
    	return new File(tempFile);
    }

    @Override
    public boolean exists(String path) {
    	return copyFile(path).exists();
    }

    @Override
    public void write(String file, byte[] bytes) {
    	try {
			FileOutputStream writer = new FileOutputStream(copyFile(file));
			writer.write(bytes);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public File toFile(String file) {
    	return copyFile(file);
    }
    
    private File copyFile(String file) {
		return new File(extraPath + file);
	}

    @Override
    public Clearcase cloneForUser(User user) throws Exception {
    	// Ignore
        return this;
    }

    @Override
    public void makeBaseline() {
    	// Ignore
    }

    protected void debug(String s) {
    	log.debug("cleartool " + s);
    }
}
