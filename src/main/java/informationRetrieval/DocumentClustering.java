package informationRetrieval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Properties;

public class DocumentClustering {

	static class ClusterAndScore{
		String clusterId;
		double score;

		public ClusterAndScore(String docId, double score) {
			this.clusterId = docId;
			this.score=score;
		}

	}
	private double curr_sim_score=10;
	private List<String> alreadyInCluster;
	static class MergePair{
		String key1;
		String key2;
		public MergePair(String key1, String key2){
			this.key1 = key1;
			this.key2 = key2;
		}

	}
	class ScoreComparator implements Comparator<ClusterAndScore>{

		@Override
		public int compare(ClusterAndScore o1, ClusterAndScore o2) {
			if(o1.score>o2.score){
				return -1;
			}else if(o1.score<o2.score) {
				return 1;
			}
			return 0;
		}
	}

	private void SimpleHAC() {
		String scoresPath = System.getProperty("user.dir")+"/src/main/java/informationRetrieval/bm25scores";
		File dir = new File(scoresPath);
		alreadyInCluster = new ArrayList<>();
		File[] propListing = dir.listFiles();
		HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix = new HashMap<>();

		//building similarity matrix.

		for(int i=0;i<propListing.length;i++) {
			PriorityQueue<ClusterAndScore> pq = new PriorityQueue<ClusterAndScore>(502, new ScoreComparator());
			for(int j=i+1;j<propListing.length;j++) {
				double score = calculateSimilarity(propListing[i],propListing[j]);
				pq.add(new ClusterAndScore(propListing[j].getName(),score));
			}
			pqsimilarityMatrix.put(propListing[i].getName(), pq);
		}
		findMostDissimilarDocuments(pqsimilarityMatrix);

		//Clustering Algorithm
		while (curr_sim_score>=0.4) {
			MergePair clustersToBeMerged = findMostSimilar(pqsimilarityMatrix);
			//merge most similar
			//System.out.println("Merged Clusters: "+clustersToBeMerged.key1+" , "+clustersToBeMerged.key2);
			mergeTwoClusters(pqsimilarityMatrix,clustersToBeMerged);
		}
		displayAllMerged();
	}

	private void findMostDissimilarDocuments(HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix) {
		double min = Double.MAX_VALUE;
		String min_1 = null;
		String min_2 = null;
		for(String key : pqsimilarityMatrix.keySet()) {
			PriorityQueue<ClusterAndScore> queue = pqsimilarityMatrix.get(key);
			ClusterAndScore min_score_of_queue_ele = getLastElementOfQueue(queue);
			if(min_score_of_queue_ele!=null && min_score_of_queue_ele.score<min) {
				min=min_score_of_queue_ele.score;
				min_1=key;
				min_2 = min_score_of_queue_ele.clusterId;
			}
		}
		System.out.println("MOST DISSIMILAR DOCUMENTS: "+min_1.replace(".properties", "")+".html and "+min_2.replace(".properties", "")+".html");

	}

	private ClusterAndScore getLastElementOfQueue(PriorityQueue<ClusterAndScore> queue) {
		int i=0;
		ClusterAndScore ele_min = null;
		for(ClusterAndScore ele: queue) {
			if(!queue.isEmpty() && ++i == queue.size()) {
				ele_min=ele;
			}
		}
		return ele_min;
	}

	private void displayAllMerged() {

		for(int i =0;i<alreadyInCluster.size();i=i+2) {
			System.out.println("1st cluster: "+alreadyInCluster.get(i).replaceAll(".properties",".html")+"; 2nd cluster: "+alreadyInCluster.get(i+1).replaceAll(".properties",".html"));
		}
	}

	private void mergeTwoClusters(HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix, MergePair clustersToBeMerged) {
		//single lingake merging
		String cluster_1 = clustersToBeMerged.key1;
		String cluster_2 = clustersToBeMerged.key2;
		PriorityQueue<ClusterAndScore> queue1 = pqsimilarityMatrix.get(cluster_1);
		for(ClusterAndScore ele : queue1) {
			if(findIfExists(ele,pqsimilarityMatrix,cluster_2)) {
				// Single linkage -- making cluster2 ready to take place of both 1 and 2
				pqsimilarityMatrix.get(cluster_2).offer(ele);
			}
		}
		pqsimilarityMatrix.put(cluster_1+","+cluster_2,pqsimilarityMatrix.get(cluster_2));
		pqsimilarityMatrix.remove(cluster_1);
		pqsimilarityMatrix.remove(cluster_2);
		removeExistenceOfOldClusters(pqsimilarityMatrix,clustersToBeMerged);
		alreadyInCluster.add(cluster_1);
		alreadyInCluster.add(cluster_2);
		//removeExistingValuesForMerged(clustersToBeMerged,pqsimilarityMatrix);
	}


	private void removeExistenceOfOldClusters(HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix,
			MergePair clustersToBeMerged) {
		String cluster_1 = clustersToBeMerged.key1;
		String cluster_2 = clustersToBeMerged.key2;


		for(String key : pqsimilarityMatrix.keySet()) {
			ClusterAndScore toBeRemoved1 = null;
			ClusterAndScore toBeRemoved2 = null;
			double more_similar = 0;
			boolean found = false;
			int both_found=0;

			PriorityQueue<ClusterAndScore> queue = pqsimilarityMatrix.get(key);
			for(ClusterAndScore ele : queue) {
				if(ele.clusterId.equals(cluster_1)) {
					//queue.remove(ele); commenting to avoind Concurrent modification exception, instead reming later
					both_found++;
					if(ele.score>more_similar) {
						more_similar= ele.score;
						found=true;
					}
					toBeRemoved1= ele;
					//pqsimilarityMatrix.get(key).remove(ele);
				}else if(ele.clusterId.equals(cluster_2)) {
					both_found++;
					if(ele.score>more_similar) {
						more_similar= ele.score;
						found = true;
					}
					toBeRemoved2 = ele;
				}
				if(both_found==2) {
					break;
				}

			}
			if(toBeRemoved1!=null) {
				pqsimilarityMatrix.get(key).remove(toBeRemoved1);
			}
			if(toBeRemoved2!=null) {
				pqsimilarityMatrix.get(key).remove(toBeRemoved2);
			}
			if(found) {
				pqsimilarityMatrix.get(key).add(new ClusterAndScore(cluster_1+","+cluster_2, more_similar));
			}
		}
	}

	private boolean findIfExists(ClusterAndScore ele1, HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix, String cluster_2) {
		PriorityQueue<ClusterAndScore> queue2 = pqsimilarityMatrix.get(cluster_2);
		for(ClusterAndScore eleInq2 : queue2) {
			if(eleInq2.clusterId.equals(ele1.clusterId)) {
				if(ele1.score>eleInq2.score) {
					//go ahead and merge
					pqsimilarityMatrix.get(cluster_2).remove(eleInq2); //remove from queue2 if better in queue1
					return true;
				}
			}
		}
		return false;
	}

	private MergePair findMostSimilar(HashMap<String, PriorityQueue<ClusterAndScore>> pqsimilarityMatrix) {
		double max = 0;
		String max_key = null;

		for(String key : pqsimilarityMatrix.keySet()) {
			if(pqsimilarityMatrix.get(key).size()!=0 && 
					pqsimilarityMatrix.get(key).peek().score>max
					&& !alreadyInCluster.contains(pqsimilarityMatrix.get(key).peek().clusterId)
					) {
				max = pqsimilarityMatrix.get(key).peek().score;
				max_key = key;		
			}
		}
		curr_sim_score = pqsimilarityMatrix.get(max_key).peek().score;
		return new MergePair(max_key, pqsimilarityMatrix.get(max_key).peek().clusterId);//max_key+","+pqsimilarityMatrix.get(max_key).peek().docId;
	}

	private double calculateSimilarity(File prop1, File prop2) {
		InputStream propStream1 = null;
		InputStream propStream2 = null;
		Properties properties1 = null;
		Properties properties2 = null;
		double dot_product = 0;
		double cosine_sim = 0;
		double mod_d1 = 0;
		double mod_d2 = 0;

		try {
			propStream1 = new FileInputStream(prop1.toString());
			propStream2 = new FileInputStream(prop2.toString());
			properties1 = new Properties();
			properties2 = new Properties();
			properties1.load(propStream1);
			properties2.load(propStream2);

		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}
		for(String key : properties1.stringPropertyNames()) {
			dot_product+= Double.parseDouble(properties1.getProperty(key,"0")) * Double.parseDouble(properties2.getProperty(key,"0"));
			mod_d1 += Math.pow(Double.parseDouble(properties1.getProperty(key)),2);
		}
		for(String key : properties2.stringPropertyNames()) {
			mod_d2 += Math.pow(Double.parseDouble(properties2.getProperty(key)),2);
		}
		mod_d1 = Math.sqrt(mod_d1);
		mod_d2 = Math.sqrt(mod_d2);
		cosine_sim = dot_product/(mod_d1*mod_d2);
		return cosine_sim;
	}

	public static void main(String[] args) {
		DocumentClustering obj = new DocumentClustering();
		obj.SimpleHAC();
	}

}
