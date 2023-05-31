package org.fsu.codeclones;

import org.dlr.foobar.SpoonBigCloneBenchDriver;

import java.util.List;

import static java.lang.Math.min;

public class LCS{
    
	public static float findPathWithLCS(List<Encoder> path, List<List<Encoder>> set){
		float lcs;
		int nodesInPath = path.size();
		float uniqueInPath;
		float uniqueInElem;
		float medium = 1.0F;

		for (List<Encoder> elem : set) {
			int nodesInElem = elem.size();
			if (SpoonBigCloneBenchDriver.bytecode)
			{
				if(Math.abs(nodesInPath - nodesInElem) < Environment.BPATHESDIFF * Math.max(nodesInPath,nodesInElem)){
					// choose the path with minimal unique Nodes
					lcs = lcs(path, elem, path.size(), elem.size());

					uniqueInPath = (nodesInPath - lcs)/(nodesInPath);
					uniqueInElem = (nodesInElem - lcs)/(nodesInElem);

					if(uniqueInPath < uniqueInElem)
						uniqueInPath = uniqueInElem;

					if(uniqueInPath < medium)
						medium = uniqueInPath;

					if(medium == 0.0){
						return 0;
					}
				}
			}
			else {
				if (Math.abs(nodesInPath - nodesInElem) < Environment.MAXDIFFNODESNO) {
					lcs = lcs(path, elem, path.size(), elem.size());

					uniqueInPath = (nodesInPath - lcs) / (nodesInPath);
					uniqueInElem = (nodesInElem - lcs) / (nodesInElem);

					if (uniqueInPath < uniqueInElem)
						uniqueInPath = uniqueInElem;


					if (uniqueInPath < medium)
						medium = uniqueInPath;


					if (medium == 0.0) {
						return 0;
					}
				}
			}
		}
		return medium;
    }
    
    static int lcs(List<Encoder> l, List<Encoder> k , int m, int n) 
    { 
        int[][] L = new int[m + 1][n + 1];
        /* Following steps build L[m+1][n+1] in bottom up fashion. Note 
         that L[i][j] contains length of LCS of X[0..i-1] and Y[0..j-1] */
	 	for (int i = 0; i <= m; i++){
	     	for(int j=0; j<= n; j++) {
                if (i == 0 || j == 0) 
                    L[i][j] = 0; 
                else if(compare(l.get(i-1).getEncoding(), k.get(j-1).getEncoding()) == 0)
					L[i][j] = L[i - 1][j - 1] + 1;
                else
                    L[i][j] = max(L[i - 1][j], L[i][j - 1]);
            } 
        } 
        return L[m][n]; 
    }

	public static float findPathWithLCS(int[] path, List<List<Encoder>> set){
		float lcs;
		int nodesInPath = path.length;
		float uniqueInPath;
		float uniqueInElem;
		float medium = 1.0F;

		for (List<Encoder> elem : set) {
			int[] path2 = elem.get(0).getEncoding();
			int nodesInElem = path2.length;

			if(Math.abs(nodesInPath - nodesInElem ) < Environment.MAXDIFFNODESNO){

				lcs = lcs(path, path2, path.length, path2.length);

				uniqueInPath = (nodesInPath - lcs)/(nodesInPath);
				uniqueInElem = (nodesInElem - lcs)/(nodesInElem);

				if(uniqueInPath < uniqueInElem)
					uniqueInPath = uniqueInElem;

				if(Environment.LINEARSUBCLONES){
					int ssize = nodesInPath;
					int lsize = nodesInElem;

					if(nodesInPath > nodesInElem){
						ssize = nodesInElem;
						lsize = nodesInPath;
					}

					if(ssize > Environment.MINLINEARSUBCLONE && lsize > ssize * Environment.LINEARFACTOR &&
							  ssize<=lcs+3) // & ssize == lcs
						uniqueInPath = (ssize - lcs)/(ssize);
				}

				if(uniqueInPath < medium){
					medium = uniqueInPath;
				}

				if(medium == 0.0){
					return 0;
				}
			}
		}
		return medium;
	}

    static int lcs(int[] l, int[] k , int m, int n) 
    { 
        int L[][] = new int[m + 1][n + 1]; 
        /* Following steps build L[m+1][n+1] in bottom up fashion. Note 
         that L[i][j] contains length of LCS of X[0..i-1] and Y[0..j-1] */
	 for (int i = 0; i <= m; i++){
	     for(int j=0; j<= n; j++) { 
                if (i == 0 || j == 0) 
                    L[i][j] = 0; 
                //else if(ModifiedHammingDistance.modifiedHammingDistance(l.get(i-1).getEncoding(), k.get(j-1).getEncoding()) <= 0.2)
                else if(l[i-1] ==  k[j-1])
		    L[i][j] = L[i - 1][j - 1] + 1; 
                else
                    L[i][j] = max(L[i - 1][j], L[i][j - 1]); 
            } 
        } 
        return L[m][n]; 
    }
    


    public static float findPathWithLCS(long[] path1_low, long[] path1_high, List<List<Encoder>> set){
	float lcs = 0;
	int nodesInPath = path1_low.length;
	float uniqueInPath = 0;
	float uniqueInElem = 0;
	float medium = 1.0F;
       
	for (List<Encoder> elem : set) {
	    long[] path2_low = ((HashEncoder)elem.get(0)).getEncoding_low();
	    long[] path2_high = ((HashEncoder)elem.get(0)).getEncoding_high();
	    
	    int nodesInElem = path2_low.length;
		
	    if(Math.abs(nodesInPath - nodesInElem ) < Environment.MAXDIFFNODESNO){
		lcs = lcs(path1_low, path1_high, path2_low, path2_high, path1_low.length, path2_low.length);
	      
	        uniqueInPath = (nodesInPath - lcs)/(nodesInPath); 
		uniqueInElem = (nodesInElem - lcs)/(nodesInElem); 
		
		if(uniqueInPath < uniqueInElem)
		    uniqueInPath = uniqueInElem;
		
		if(uniqueInPath < medium){
		    medium = uniqueInPath;
		}
	    
		if(medium == 0.0){
		    //System.out.println("Medium");   
		    return 0;
		}
	    }
	}
	    
	return medium;
    }

    static int lcs(long[] l_low, long[] l_high, long[] k_low , long[] k_high, int m, int n) 
    { 
        int[][] L = new int[m + 1][n + 1];
        /* Following steps build L[m+1][n+1] in bottom up fashion. Note 
         that L[i][j] contains length of LCS of X[0..i-1] and Y[0..j-1] */
	 for (int i = 0; i <= m; i++){
	     for(int j=0; j<= n; j++) { 
                if (i == 0 || j == 0) 
                    L[i][j] = 0; 
                //else if(ModifiedHammingDistance.modifiedHammingDistance(l.get(i-1).getEncoding(), k.get(j-1).getEncoding()) <= 0.2)
                else if(l_low[i-1] ==  k_low[j-1] && l_high[i-1] ==k_high[j-1])
		    L[i][j] = L[i - 1][j - 1] + 1; 
                else
                    L[i][j] = max(L[i - 1][j], L[i][j - 1]); 
            } 
        } 
        return L[m][n]; 
    }
    
  
    /* Utility function to get max of 2 integers */
    static int max(int a, int b)
    { 
        return Math.max(a, b);
    } 
  
    public static float compare(int[] a1, int[] a2){

        if(a1.length != a2.length){return 1;}
      
		for(int i = 0; i < a1.length; i++){
			if(a1[i] != a2[i]){return 1;}
		}
	
		return 0;
    }	    
}
