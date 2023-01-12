package org.fsu.codeclones;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class LevenShtein{
    
	static float findPathWithLEVENSHTEIN(List<Encoder> path, List<List<Encoder>> set){
	float levenShtein = 0;
	int nodesInPath = path.size();
	
	float medium = 1.0F;
	
	for (List<Encoder> elem : set) {

	    int nodesInElem = elem.size();

	    if(Math.abs(nodesInPath - nodesInElem ) < Environment.MAXDIFFNODESNO){ 	

		int size1 = nodesInPath;
		int size2 = nodesInElem;
	    
		if(nodesInPath >  nodesInElem){
		    size2 = nodesInPath;
		    size1 = nodesInElem;
		}

		levenShtein = levenshtein(path, elem); // alte Werte 4 4 siz2-size1

		if(levenShtein == 0)
		    return 0;
	    
		if(levenShtein/size2 < medium)
		    medium = levenShtein/size2;
		
	    }
	}
	
	return medium;
    }
    
    /**
     * Calculate the Levenshtein distance between two paths. Basically, the number of
     * changes that need to be made to convert one path into another.  
     * @param pathOne
     * @param pathTwo 
     * @return The Levenshtein distance
     */
    public static int levenshtein(List<Encoder> pathOne, List<Encoder> pathTwo)
    {
        
        // store length
        int m = pathOne.size();
        int n = pathTwo.size();
        
        // matrix to store differences
        int[][] deltaM = new int[m+1][n+1];
        
        for(int i = 1;i <= m; i++)
        {
            deltaM[i][0] = i;        
        }
        
        for(int j = 1;j <= n; j++)
        {
            deltaM[0][j] = j;
        }
        
        for(int j=1;j<=n;j++)
        {
            for(int i=1;i<=m;i++)
            {
                //if(ModifiedHammingDistance.modifiedHammingDistance(pathOne.get(i-1).getEncoding(),
                if(compare(pathOne.get(i-1).getEncoding(),
			   pathTwo.get(j-1).getEncoding()) == 0F)
                {
                    deltaM[i][j] = deltaM[i-1][j-1];                    
                }
                else
                {
                    deltaM[i][j] = Math.min(
                            deltaM[i-1][j]+1, 
                            Math.min(
                                    deltaM[i][j-1]+1, 
                                    deltaM[i-1][j-1]+1
                            )
                    );
                }                
            }    
        }
        
        return deltaM[m][n];       
    
    }

     static final float compare(int[] a1, int[] a2){

        if(a1.length != a2.length)
	    return 1;
      
	for(int i = 0; i < a1.length; i++)
	    if(a1[i] != a2[i])
		return 1;
	
	return 0;
    }

    static float findPathWithLEVENSHTEIN(int[] path, List<List<Encoder>> set){
	float levenShtein = 0;
	int nodesInPath = path.length;
	float medium = 1.0F;
	
	for (List<Encoder> elem : set) {

	    int[] path2 = elem.get(0).getEncoding();
	    int nodesInElem = path2.length;
	    
	    if(Math.abs(nodesInPath - nodesInElem ) < Environment.MAXDIFFNODESNO){ 	
		int size1 = nodesInPath;
		int size2 = nodesInElem;
		
		if(nodesInPath >  nodesInElem){
		    size2 = nodesInPath;
		    size1 = nodesInElem;
		}

		levenShtein = levenshtein(path, path2); // alte Werte 4 4 siz2-size1

		if(levenShtein == 0)
		    return 0;
	    
		if(levenShtein/size2 < medium)
		    medium = levenShtein/size2;

	   
	    }
	}
	
	return medium;
    }
    

    static int levenshtein(int[] pathOne, int[] pathTwo) 
    {

        
        // store length
        int m = pathOne.length;
        int n = pathTwo.length;
        
        // matrix to store differences
        int[][] deltaM = new int[m+1][n+1];
        
        for(int i = 1;i <= m; i++)
        {
            deltaM[i][0] = i;        
        }
        
        for(int j = 1;j <= n; j++)
        {
            deltaM[0][j] = j;
        }
        
        for(int j=1;j<=n;j++)
        {
            for(int i=1;i<=m;i++)
            {
                //if(ModifiedHammingDistance.modifiedHammingDistance(pathOne.get(i-1).getEncoding(),
                if(pathOne[i-1] == pathTwo[j-1])
                {
                    deltaM[i][j] = deltaM[i-1][j-1];                    
                }
                else
                {
                    deltaM[i][j] = Math.min(
                            deltaM[i-1][j]+1, 
                            Math.min(
                                    deltaM[i][j-1]+1, 
                                    deltaM[i-1][j-1]+1
                            )
                    );
                }                
            }    
        }
        
        return deltaM[m][n];       
    
    }
    
 static float findPathWithLEVENSHTEIN(long[] path1_low, long[] path1_high, List<List<Encoder>> set){

	float levenShtein = 0;
	int nodesInPath = path1_low.length;
	float medium = 1.0F;
	
	for (List<Encoder> elem : set) {
	    long[] path2_low = ((HashEncoder)elem.get(0)).getEncoding_low();
	    long[] path2_high = ((HashEncoder)elem.get(0)).getEncoding_high();
	    
	    int nodesInElem = path2_low.length;
	    	    
	    if(Math.abs(nodesInPath - nodesInElem ) < Environment.MAXDIFFNODESNO){ 	
		int size1 = nodesInPath;
		int size2 = nodesInElem;
		
		if(nodesInPath >  nodesInElem){
		    size2 = nodesInPath;
		    size1 = nodesInElem;
		}

		levenShtein = levenshtein(path1_low, path1_high, path2_low, path2_high); // alte Werte 4 4 siz2-size1

		if(levenShtein == 0)
		    return 0;
	    
		if(levenShtein/size2 < medium)
		    medium = levenShtein/size2;

	   
	    }
	}
	
	return medium;
     
    }

	
 static int levenshtein(long[] pathOne_low, long[] pathOne_high, long[] pathTwo_low , long[] pathTwo_high) 
    { 


        // store length
        int m = pathOne_low.length;
        int n = pathTwo_low.length;
        
        // matrix to store differences
        int[][] deltaM = new int[m+1][n+1];
        
        for(int i = 1;i <= m; i++)
        {
            deltaM[i][0] = i;        
        }
        
        for(int j = 1;j <= n; j++)
        {
            deltaM[0][j] = j;
        }
        
        for(int j=1;j<=n;j++)
        {
            for(int i=1;i<=m;i++)
            {
                //if(ModifiedHammingDistance.modifiedHammingDistance(pathOne.get(i-1).getEncoding(),
                if(pathOne_low[i-1] ==  pathTwo_low[j-1] && pathOne_high[i-1] == pathTwo_high[j-1])
                {
                    deltaM[i][j] = deltaM[i-1][j-1];                    
                }
                else
                {
                    deltaM[i][j] = Math.min(
                            deltaM[i-1][j]+1, 
                            Math.min(
                                    deltaM[i][j-1]+1, 
                                    deltaM[i-1][j-1]+1
                            )
                    );
                }                
            }    
        }
        
        return deltaM[m][n];       
    
    }
    
}

