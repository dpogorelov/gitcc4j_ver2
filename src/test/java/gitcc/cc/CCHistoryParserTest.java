package gitcc.cc;

import gitcc.cc.CCFile.Status;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

public class CCHistoryParserTest extends TestCase {

	public void test() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		StringBuilder b = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream("lshistory.example")));
		for (String line; (line = in.readLine()) != null;)
			b.append(line).append("\n");
		String s = b.toString();
		Collection<CCCommit> commits = parse(s, 6, "charleso_dev");
		parse(s, 3, "dev_int");
		parse(s, 43, "charleso_dev", "dev_int");
		CCCommit first = commits.iterator().next();
		assertEquals("charleso", first.getAuthor());
		assertEquals("Ellipse build for everything", first.getMessage());
		assertEquals(1244125636000l, first.getDate().getTime());
		List<CCFile> files = first.getFiles();
		assertEquals(1, files.size());
		CCFile file = files.get(0);
		assertEquals("prod/somepath/build.xml", file.getFile());
		assertEquals("charleso_dev", file.getVersion().getBranch());
		assertEquals(Status.Added, file.getStatus());
	}

	private Collection<CCCommit> parse(String s, int expected,
			String... branches) {
		Collection<CCCommit> commits = new CCHistoryParser().parse(s, branches);
		assertEquals(expected, commits.size());
		return commits;
	}
}
