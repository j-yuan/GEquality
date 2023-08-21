import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GEquality {
	final static String FILE_NAME = "wikivote.txt";
	final static int V = 8298; // wikivote:V=8298,edge=103689,nethept:V=15229,edge=58891,netphy:V=37154,edge=231584;epinions:V=75888,edge=508837
	final static int sampleNum = 10;
	final static double p = 0.99;
	static double edgeWeight = 0.01; // 0.05 [0.01, 0.2]
	static int numGroup = 20; // 20 when test this (6,8)
	static int alpha = 0; // equality threshold
	static double expActSize = 0;
	static double expActSizeBase = 0;
	static Set<Integer>[] groupList = new HashSet[numGroup];
	static int minGroupSize;
	static int minGroupID;
	static int[] groupSizeFeas = new int[numGroup];
	static int[] groupID = new int[V];
	static Integer[] groupSizeSeed = new Integer[numGroup];
	static Set<Integer>[] fullAdjList = new HashSet[V];
	static Set<Integer>[][] sampleAdjListsOriginal = new HashSet[sampleNum + 1][V];
	static Set<Integer>[][] sampleAdjLists = new HashSet[sampleNum + 1][V];
	static Set<Integer>[][] singleNodeReachableSet = new HashSet[sampleNum + 1][V];
	static Set<Integer> candSet = new HashSet<Integer>();
	static Set<Integer> seedSet = new HashSet<Integer>();
	static Set<Integer> seedSetAdapt = new HashSet<Integer>();
	static Set<Integer> seedSetBase = new HashSet<Integer>();
	static Set<Integer> seedSetBaseAdpt = new HashSet<Integer>();
	static Set<Integer> actSet = new HashSet<Integer>(); // adpt
	static Set<Integer> actSetBase = new HashSet<Integer>(); // adpt base
	static Set<Integer>[] sampleReachableSet = new HashSet[sampleNum + 1]; // current
																			// seeds'
																			// reachable
																			// set-non-adaptive

	public static void main(String[] args) throws IOException {
		ReadFile text = new ReadFile(V);
		text.initializeGraph();
		text.readLargerTextFile(FILE_NAME);
		genGroupList();
		genFullAdjList(text.adjList);
		genSampleAdjList();
		genSingleNodeReachableSet();
		genSampleReachableSet();
		reduceCandidateSet();

		setSampleAdjList();
		setSingleNodeReachableSet();

		for (int gnum = 3; gnum < 12; gnum+=3) {
			numGroup = gnum;

			setGroupsRandom();
//			setGroupsGaussian();
//			for (int ew = 2; ew < 12; ew++) {
//			for (int i=0; i<3; i++) {
				alpha = 0;
//				if(i==1)
//					alpha = 10;
//				else if(i==2)
//					alpha = 50;
				log("");
				log("********************alpha = " + alpha + ":");
				getGroupSizeFeasible();
//				greedy();
//				greedyAdpt();
//				getMinGroupID();
//				baseline();
//				baselineAdpt();

//			}

//			edgeWeight = ew * 0.01;
//			setSampleAdjList();
//			setSingleNodeReachableSet();
		}

	}
	
	public static void baselineAdpt() {
		clearGroupSizeSeed();
		int upbound = minGroupSize + alpha;
		groupSizeSeed[minGroupID] = minGroupSize;
		Set<Integer> cand = new HashSet<Integer>();
		Set<Integer> reachableSet = new HashSet<Integer>();
		seedSetBaseAdpt.clear();
		actSetBase.clear();
		resetSampleAdjList();
		cand.addAll(candSet);
		seedSetBaseAdpt.addAll(groupList[minGroupID]);
		cand.removeAll(seedSetBaseAdpt);
		
		for(int b : seedSetBaseAdpt) {
			actSetBase.addAll(singleNodeReachableSet[0][b]);
		}
		
		while(!cand.isEmpty()) {
			int bestmc = 0;
			int bestv = -1;
			int bestvGroup;
			for (int i : cand) {
				int mc = 0;
				for (int s = 0; s <= sampleNum; s++) {
					reachableSet.addAll(reachSetSingleNode(i, s));
					reachableSet.removeAll(actSetBase);
					mc += reachableSet.size();
					reachableSet.clear();
				}
				if(mc > bestmc) {
					bestmc = mc;
					bestv = i;
				}
			}
			if (((double) bestmc / (sampleNum + 1)) > 1) {
				bestvGroup = groupID[bestv];
				if (groupSizeSeed[bestvGroup] < upbound) {
					seedSetBaseAdpt.add(bestv);
					groupSizeSeed[bestvGroup]++;
					actSetBase.addAll(reachSetSingleNode(bestv, 0));
					updateFeedback(bestv);
				}
				cand.remove(bestv);
			} else {
				break;
			}
		}
		
		log("[BaseAdpt] seedSetBaseAdpt size: " + seedSetBaseAdpt.size());
		log("[BaseAdpt] realized utility baseAdpt: " + (actSetBase.size() - seedSetBaseAdpt.size()));
		for(int g=0; g<numGroup; g++)
			log("group " + g + ":" + groupSizeSeed[g]);
		
		seedSetBaseAdpt.addAll(makeSeedFeasibleBase(true));
		log("[BaseAdpt] feasible seedSetBaseAdpt size: " +seedSetBaseAdpt.size());
		log("[BaseAdpt] feasible expected utility baseAdpt: " + (actSetBase.size() - seedSetBaseAdpt.size()));
		for(int g=0; g<numGroup; g++){
			log("group " + g + ": " + groupSizeSeed[g]);
		}
		
	}
	
	public static void baseline() {
		clearGroupSizeSeed();
		int upbound = minGroupSize + alpha;
		groupSizeSeed[minGroupID] = minGroupSize;
		for(int g=0; g<numGroup; g++){
		log("begin group size: "+groupSizeSeed[g]);
		}
		Set<Integer> cand = new HashSet<Integer>();
		Set<Integer> reachableSet = new HashSet<Integer>();
		seedSetBase.clear();
		clearSampleReachableSet();
		resetSampleAdjList();
		cand.addAll(candSet);			
		seedSetBase.addAll(groupList[minGroupID]);
		cand.removeAll(seedSetBase);
		
		for(int s=0; s<= sampleNum; s++) {
			for(int b : seedSetBase) {
				sampleReachableSet[s].addAll(singleNodeReachableSet[s][b]);
			}
		}
		
		while(!cand.isEmpty()) {
			int bestmc = 0;
			int bestv = -1;
			int bestvGroup;
			for (int i : cand) {
				int mc = 0;
				for (int s = 0; s <= sampleNum; s++) {
					reachableSet.addAll(singleNodeReachableSet[s][i]);
					reachableSet.removeAll(sampleReachableSet[s]);
					mc += reachableSet.size();
					reachableSet.clear();
				}
				if (mc > bestmc) {
					bestmc = mc;
					bestv = i;
				}
			}
			
			if (((double) bestmc / (sampleNum + 1)) > 1) {
				bestvGroup = groupID[bestv];
				if (groupSizeSeed[bestvGroup] < upbound) {
					seedSetBase.add(bestv);
					groupSizeSeed[bestvGroup]++;
					for (int s = 0; s <= sampleNum; s++) {
						sampleReachableSet[s].addAll(singleNodeReachableSet[s][bestv]);
					}
				}
				cand.remove(bestv);
			} else {
				break;
			}
		}
		
		log("[Base] seedSetBase size: " + seedSetBase.size());
		expActSizeBase = 0;
		for(int s = 0; s<=sampleNum; s++){
			expActSizeBase += sampleReachableSet[s].size();
			log("sample reachable set "+s+": "+sampleReachableSet[s].size());
		}
		expActSizeBase /= (sampleNum + 1);
		log("[Base] expected utility base: " + (expActSizeBase - seedSetBase.size()));
		for(int g=0; g<numGroup; g++)
			log("group " + g + ":" + groupSizeSeed[g]);
		
		seedSetBase.addAll(makeSeedFeasibleBase(false));
		log("[Base] feasible seedSetBase size: " +seedSetBase.size());
		log("[Base] feasible expected utility: " + (expActSizeBase - seedSetBase.size()));
		for(int g=0; g<numGroup; g++){
			log("group " + g + ": " + groupSizeSeed[g]);
		}
		
	}
	
	public static Set<Integer> makeSeedFeasibleBase(boolean adptFlag) {
		Set<Integer> makeupSet = new HashSet<Integer>();
		log("in feasible, min group id: "+minGroupID);
		for (int g=0; g<numGroup; g++) {
			if(groupSizeSeed[g] < minGroupSize){
				int diff = minGroupSize - groupSizeSeed[g];
				Set<Integer> cands = new HashSet<Integer>();
				cands.addAll(groupList[g]);
				if(!adptFlag)
					cands.removeAll(seedSetBase);
				else
					cands.removeAll(seedSetBaseAdpt);
				for(int c : cands) {
					makeupSet.add(c);
					groupSizeSeed[g]++;
					diff--;
					if(diff == 0)
						break;
				}
			}
		}
		
		if (!adptFlag) {
			expActSizeBase = 0;
			for (int s=0; s<=sampleNum; s++) {
				for (int m : makeupSet) {
					sampleReachableSet[s].addAll(singleNodeReachableSet[s][m]);
				}
				expActSizeBase += sampleReachableSet[s].size();
			}
			expActSizeBase /= (sampleNum + 1);
		} else {
			for (int m : makeupSet) {
				actSetBase.addAll(reachSetSingleNode(m, 0));
			}
		}
		
		return makeupSet;
	}		
		

	public static void greedy() {
		Set<Integer> cand = new HashSet<Integer>();
		Set<Integer> reachableSet = new HashSet<Integer>();
		seedSet.clear();
		clearSampleReachableSet();
		clearGroupSizeSeed();
		resetSampleAdjList();
		cand.addAll(candSet);

		while (!cand.isEmpty()) {
			int bestmc = 0;
			int bestv = -1;
			int bestvGroup;
			for (int i : cand) {
				int mc = 0;
				for (int s = 0; s <= sampleNum; s++) {
					reachableSet.addAll(singleNodeReachableSet[s][i]);
					reachableSet.removeAll(sampleReachableSet[s]);
					mc += reachableSet.size();
					reachableSet.clear();
				}
				if (mc > bestmc) {
					bestmc = mc;
					bestv = i;
				}
			}

			if (((double) bestmc / (sampleNum + 1)) > 1) {
				bestvGroup = groupID[bestv];
				if (groupSizeSeed[bestvGroup] < groupSizeFeas[bestvGroup]) {
					seedSet.add(bestv);
					groupSizeSeed[bestvGroup]++;
					for (int s = 0; s <= sampleNum; s++) {
						sampleReachableSet[s].addAll(singleNodeReachableSet[s][bestv]);
					}
				}
				cand.remove(bestv);
			} else {
				break;
			}

		}

		log("seedSet size: " + seedSet.size());
		expActSize = 0;
		for (int s = 0; s <= sampleNum; s++)
			expActSize += sampleReachableSet[s].size();
		expActSize /= (sampleNum + 1);
		log("expected utility: " + (expActSize - seedSet.size()));
		for (int g = 0; g < numGroup; g++)
			log("group " + g + ": " + groupSizeSeed[g]);

		Set<Integer> seedsFeas = new HashSet<Integer>();
		seedsFeas = makeSeedFeasible(false);
		log("feasible seedSet size: " + seedsFeas.size());
		log("feasible expected utility: " + (expActSize - seedsFeas.size()));
		for (int g = 0; g < numGroup; g++) {
			log("group " + g + ": " + groupSizeSeed[g]);
		}
	}

	public static void greedyAdpt() {
		seedSetAdapt.clear();
		actSet.clear();
		clearGroupSizeSeed();
		resetSampleAdjList();

		Set<Integer> cand = new HashSet<Integer>();
		cand.addAll(candSet);
		while (!cand.isEmpty()) {
			int bestmc = 0;
			int bestv = -1;
			int bestvGroup;
			Set<Integer> reachableSet = new HashSet<Integer>();
			for (int i : cand) {
				int mc = 0;
				for (int s = 0; s <= sampleNum; s++) {
					reachableSet.addAll(reachSetSingleNode(i, s));
					reachableSet.removeAll(actSet);
					mc += reachableSet.size();
					reachableSet.clear();
				}
				if (mc > bestmc) {
					bestmc = mc;
					bestv = i;
				}
			}

			if ((double) bestmc / (sampleNum + 1) > 1) {
				bestvGroup = groupID[bestv];
				if (groupSizeSeed[bestvGroup] < groupSizeFeas[bestvGroup]) {
					seedSetAdapt.add(bestv);
					groupSizeSeed[bestvGroup]++;
					actSet.addAll(reachSetSingleNode(bestv, 0));
					updateFeedback(bestv);
				}
				cand.remove(bestv);
			} else {
				break;
			}
		}

		log("Adaptive seedSet size: " + seedSetAdapt.size());
		log("realized utility: " + (actSet.size() - seedSetAdapt.size()));
		for (int g = 0; g < numGroup; g++)
			log("group " + g + ": " + groupSizeSeed[g]);
		Set<Integer> seedsFeasAdpt = new HashSet<Integer>();
		seedsFeasAdpt = makeSeedFeasible(true);
		log("Adaptive feasible seedSet size: " + seedsFeasAdpt.size());
		log("feasible expected utility: " + (actSet.size() - seedsFeasAdpt.size()));
		for (int g = 0; g < numGroup; g++)
			log("group " + g + ": " + groupSizeSeed[g]);
	}

	public static void random() {
		Random ran = new Random();
		Set<Integer> seedSetRand = new HashSet<Integer>();
		Set<Integer> reachableSet = new HashSet<Integer>();
		seedSetRand.addAll(groupList[minGroupID]);
		getMinGroupID();
		for (int g = 0; g < numGroup; g++) {
			if (g != minGroupID) {
				int c = minGroupSize + ran.nextInt(alpha + 1);
				for (int i : groupList[g]) {
					seedSetRand.add(i);
					c--;
					if (c == 0)
						break;
				}
			}
		}

		double expActSizeRand = 0;
		for (int s = 0; s <= sampleNum; s++) {
			for (int r : seedSetRand) {
				reachableSet.addAll(singleNodeReachableSet[s][r]);
			}
			expActSizeRand += reachableSet.size();
			reachableSet.clear();
		}
		expActSizeRand /= (sampleNum + 1);
		log("[Random] seedset size: " + seedSetRand.size());
		log("[Random] expected utility: " + (expActSizeRand - seedSetRand.size()));
	}

	// get min_group_id for baseline
	public static void getMinGroupID() {
		int min = groupList[0].size();
		minGroupID = 0;
		// Collections.min(Arrays.asList(groupList));
		for (int g = 1; g < numGroup; g++) {
			if (groupList[g].size() < min) {
				min = groupList[g].size();
				minGroupID = g;
			}
		}
		minGroupSize = min;
	}

	public static Set<Integer> makeSeedFeasible(boolean adptFlag) {
		Set<Integer> seeds = new HashSet<Integer>();
		if (adptFlag)
			seeds.addAll(seedSetAdapt);
		else
			seeds.addAll(seedSet);

		int min = groupSizeSeed[0];
		int max = groupSizeSeed[0];
		// groupSizeSeed created with larger numGroup when its tested
		// Collections.min(Arrays.asList(groupSizeSeed));
		for (int g = 1; g < numGroup; g++) {
			if (groupSizeSeed[g] < min) {
				min = groupSizeSeed[g];
			}
			if (groupSizeSeed[g] > max)
				max = groupSizeSeed[g];
		}

		if (max - min > alpha) {
			Set<Integer>[] Xi = new HashSet[numGroup];
			Set<Integer>[] Yi = new HashSet[numGroup];
			for (int g = 0; g < numGroup; g++) {
				Xi[g] = new HashSet<Integer>();
				Yi[g] = new HashSet<Integer>();
			}
			for (int g = 0; g < numGroup; g++) {
				int diff = max - groupSizeSeed[g];
				if (diff > alpha) {
					Set<Integer> cands = new HashSet<Integer>();
					cands.addAll(groupList[g]);
					cands.removeAll(seeds);

					int d = diff - alpha;
					if (cands.size() < d * 2)
						log("*****************alert: not sufficient items to make up*************");

					for (int c : cands) {
						Xi[g].add(c);
						d--;
						if (d == 0)
							break;
					}

					cands.removeAll(Xi[g]);
					d = diff - alpha;
					for (int c : cands) {
						Yi[g].add(c);
						d--;
						if (d == 0)
							break;
					}

				}
			}

			// compare union Xi and union Yi
			Set<Integer> X = new HashSet<Integer>();
			Set<Integer> Y = new HashSet<Integer>();
			for (int g = 0; g < numGroup; g++) {
				X.addAll(Xi[g]);
				Y.addAll(Yi[g]);
			}

			if (!adptFlag) {
				Set<Integer> reachSetX = new HashSet<Integer>();
				Set<Integer> reachSetY = new HashSet<Integer>();
				int mcX = 0;
				int mcY = 0;

				for (int s = 0; s <= sampleNum; s++) {
					for (int x : X) {
						reachSetX.addAll(singleNodeReachableSet[s][x]);
					}
					reachSetX.removeAll(sampleReachableSet[s]);
					mcX += reachSetX.size();
					reachSetX.clear();
				}
				for (int s = 0; s <= sampleNum; s++) {
					for (int y : Y) {
						reachSetY.addAll(singleNodeReachableSet[s][y]);
					}
					reachSetY.removeAll(sampleReachableSet[s]);
					mcY += reachSetY.size();
					reachSetY.clear();
				}

				if (mcX > mcY) {
					seeds.addAll(X);
					expActSize = 0;
					for (int s = 0; s <= sampleNum; s++) {
						for (int x : X) {
							sampleReachableSet[s].addAll(singleNodeReachableSet[s][x]);
						}
						expActSize += sampleReachableSet[s].size();
					}
					expActSize /= (sampleNum + 1);
					for (int x : X)
						groupSizeSeed[groupID[x]]++;
				} else {
					seeds.addAll(Y);
					expActSize = 0;
					for (int s = 0; s <= sampleNum; s++) {
						for (int y : Y) {
							sampleReachableSet[s].addAll(singleNodeReachableSet[s][y]);
						}
						expActSize += sampleReachableSet[s].size();
					}
					expActSize /= (sampleNum + 1);
					for (int y : Y)
						groupSizeSeed[groupID[y]]++;
				}
			} else {
				Set<Integer> reachSetX = new HashSet<Integer>();
				Set<Integer> reachSetY = new HashSet<Integer>();
				int mcX = 0;
				int mcY = 0;
				for (int s = 0; s <= sampleNum; s++) {
					for (int x : X) {
						reachSetX.addAll(reachSetSingleNode(x, s));
					}
					reachSetX.removeAll(actSet);
					mcX += reachSetX.size();
					reachSetX.clear();
				}
				for (int s = 0; s <= sampleNum; s++) {
					for (int y : Y) {
						reachSetY.addAll(reachSetSingleNode(y, s));
					}
					reachSetY.removeAll(actSet);
					mcY += reachSetY.size();
					reachSetY.clear();
				}
				if (mcX > mcY) {
					seeds.addAll(X);
					for (int x : X) {
						actSet.addAll(reachSetSingleNode(x, 0));
					}
					for (int x : X)
						groupSizeSeed[groupID[x]]++;
				} else {
					seeds.addAll(Y);
					for (int y : Y) {
						actSet.addAll(reachSetSingleNode(y, 0));
					}
					for (int y : Y)
						groupSizeSeed[groupID[y]]++;
				}
			}
		}

		return seeds;
	}

	public static void updateFeedback(int node) {
		Queue<Integer> q = new Queue<Integer>();
		boolean[] marked = new boolean[V];
		marked[node] = true;
		q.enqueue(node);
		while (!q.isEmpty()) {
			int v = q.dequeue();
			for (int s = 1; s <= sampleNum; s++) {
				sampleAdjLists[s][v] = sampleAdjLists[0][v];
			}
			for (int n : sampleAdjLists[0][v]) {
				if (!marked[n]) {
					marked[n] = true;
					q.enqueue(n);
				}
			}
		}
	}

	public static void reduceCandidateSet() {
		candSet.clear();
		for (int i = 0; i < V; i++) {
			if (Math.random() <= p) {
				candSet.add(i);
			}
		}
	}

	public static void getGroupSizeFeasible() {
		log("group size : ");
		Integer[] groupSize = new Integer[numGroup];
		for (int g = 0; g < numGroup; g++) {
			groupSize[g] = groupList[g].size();
			log("group " + g + ": " + groupSize[g]);
		}

		int kmin = Collections.min(Arrays.asList(groupSize));
		log("kmin : " + kmin);
		for (int g = 0; g < numGroup; g++) {
			groupSizeFeas[g] = Math.min((int) (groupSize[g] / 2), (int) (kmin / 2 + alpha));
			log("group " + g + " size feas: " + groupSizeFeas[g]);
		}
	}

	public static void setGroupsRandom() {
		clearGroups();
		int g;
		Random ran = new Random();
		for (int i = 0; i < V; i++) {
			g = ran.nextInt(numGroup);
			groupList[g].add(i);
			groupID[i] = g;
		}
	}

	public static void setGroupsGaussian() {
		if (numGroup == 2) {
			setGroupsRandom();
			return;

		}

		clearGroups();

		int g;
		double r;
		int mean = numGroup / 2;
		Random ran = new Random();
		for (int i = 0; i < V; i++) {
			r = ran.nextGaussian()*2.8 + mean; //*Math.sqrt(0.5)
			g = (int) r;
			if (g < 0)
				g = 0;
			if (g >= numGroup)
				g = numGroup - 1;
			groupList[g].add(i);
			groupID[i] = g;
		}
	}

	public static Set<Integer> reachSetSingleNode(int node, int s) {
		Set<Integer> reachSet = new HashSet<Integer>();
		// compute reachable set from node j
		Queue<Integer> qu = new Queue<Integer>();
		boolean[] marked = new boolean[V];
		marked[node] = true;
		reachSet.add(node);
		qu.enqueue(node);

		while (!qu.isEmpty()) {
			int v = qu.dequeue();
			for (int w : sampleAdjLists[s][v]) {
				if (!marked[w]) {
					marked[w] = true;
					reachSet.add(w);
					qu.enqueue(w);
				}
			}
		}

		return reachSet;
	}

	public static void clearGroupSizeSeed() {
		for (int g = 0; g < numGroup; g++) {
			groupSizeSeed[g] = 0;
		}
	}

	public static void clearGroups() {
		for (int g = 0; g < numGroup; g++)
			groupList[g].clear();
	}

	public static void clearSampleReachableSet() {
		for (int i = 0; i <= sampleNum; i++) {
			sampleReachableSet[i].clear();
		}
	}

	public static void setSingleNodeReachableSet() {
		for (int s = 0; s <= sampleNum; s++) {
			for (int i = 0; i < V; i++) {
				singleNodeReachableSet[s][i].clear();
				singleNodeReachableSet[s][i].addAll(reachSetSingleNode(i, s));
			}
		}
	}

	public static void resetSampleAdjList() {
		for (int s = 0; s <= sampleNum; s++) {
			for (int i = 0; i < V; i++) {
				sampleAdjLists[s][i] = sampleAdjListsOriginal[s][i];
			}
		}
	}

	public static void setSampleAdjList() {
		for (int s = 0; s <= sampleNum; s++) {
			for (int i = 0; i < V; i++) {
				sampleAdjListsOriginal[s][i].clear();
				for (int j : fullAdjList[i]) {
					if (Math.random() <= edgeWeight) {
						sampleAdjListsOriginal[s][i].add(j);
					}
				}
				sampleAdjLists[s][i] = sampleAdjListsOriginal[s][i];
			}
		}
	}

	public static void genSampleReachableSet() {
		for (int i = 0; i <= sampleNum; i++) {
			sampleReachableSet[i] = new HashSet<Integer>();
		}
	}

	public static void genSingleNodeReachableSet() {
		for (int s = 0; s <= sampleNum; s++) {
			for (int i = 0; i < V; i++) {
				singleNodeReachableSet[s][i] = new HashSet<Integer>();
			}
		}
	}

	public static void genSampleAdjList() {
		for (int s = 0; s <= sampleNum; s++) {
			for (int i = 0; i < V; i++) {
				sampleAdjListsOriginal[s][i] = new HashSet<Integer>();
				sampleAdjLists[s][i] = new HashSet<Integer>();
			}
		}
	}

	public static void genFullAdjList(Set<Integer>[] aList) {
		for (int i = 0; i < V; i++) {
			fullAdjList[i] = new HashSet<Integer>();
			fullAdjList[i].addAll(aList[i]);
		}
	}

	public static void genGroupList() {
		for (int g = 0; g < numGroup; g++) {
			groupList[g] = new HashSet<Integer>();
		}
	}

	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

}
