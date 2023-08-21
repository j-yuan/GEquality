import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ReadFile {
	int VNUM;
	Set<Integer>[] adjList;
	final static Charset ENCODING = StandardCharsets.UTF_8;

	public ReadFile(int V) {
		VNUM = V;
		adjList = new HashSet[VNUM];
		// edgeWeight = new double[VNUM][VNUM];
	}

	void readLargerTextFile(String aFileName) throws IOException {
		Path path = Paths.get(aFileName);
		Random r = new Random();
		try (BufferedReader reader = Files.newBufferedReader(path, ENCODING)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				// process each line
				String[] tokenize = line.split("	");
				int from = Integer.parseInt(tokenize[0]);
				int to = Integer.parseInt(tokenize[1]);
				adjList[from].add(to);

				// edge weight
				// double weight = Double.parseDouble(tokenize[2]);
				// if (r.nextInt(2) == 1) {
				// edgeWeight[from][to] = 0.01;
				// } else {
				// edgeWeight[from][to] = 0.01;
				// }
			}
		}

	}

	void initializeGraph() {
		for (int i = 0; i < VNUM; i++) {
			adjList[i] = new HashSet<Integer>();
			// edgeWeight[i][j] = 0.0;
		}
	}

	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

}
