package org.fsu.codeclones;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class HammingDistance{

    static float THRESHOLD = (float) 0;
    static boolean RELATIV = true;
    static boolean SORTED = false;
	
    static int hammingDistance(int[] a1, int[] a2){
	int distance = 0;
	 
	int size = a1.length;

	//Determine lower size and set the initial value of distance
	if(size > a2.length){
	    distance += size - a2.length;
	    size = a2.length; 
	} else{
	    if(size < a2.length)
		distance += a2.length - size;
	}
	
	for(int i=0; i < size; i++)
	    if(a1[i] != a2[i])
		distance++;
	
	//System.out.println("D " +distance);
	return distance;
    }

   
    static int hammingDistanceForTwoPaths(List<Encoder> l, List<Encoder> k, boolean unequalSizePenalty) {

	if(l == null || k== null)
	    throw new IllegalArgumentException();
	
	List<Encoder> sl = l;
	List<Encoder> ll = k;
	int distance = 0;
	float tmp, tmp1;
	if(l.size() > k.size()){
	    sl = k;
	    ll = l;
	}
	int i=0;
	
	//System.out.println("Size " + sl.size());

	// ToDo: Abbrechen wenn man weiss, dass die Pfade nicht gleich sind
	try{
	    for (i = 0; i < sl.size(); i++){
		tmp = 1000;
		for(int j=0; j< ll.size(); j++) {
		    tmp1 = hammingDistance(sl.get(i).getEncoding(),ll.get(j).getEncoding());
		    if(tmp1 < tmp)
			tmp = tmp1;
		//System.out.println(sl.get(i));
		//System.out.println(ll.get(i));
		//System.out.println(distance);
		}
		distance+=tmp;
	    }
	}
	catch(Exception e){
	    e.printStackTrace();
	}
	
  	if(unequalSizePenalty)
	    for (; i < ll.size(); i++){
		distance += ll.get(i).getEncoding().length;
		//System.out.println(sl.get(i));
		//System.out.println(ll.get(i));
		//System.out.println("Penalty " + distance);
	    }
	//System.out.println(distance);
	return distance;
    }

    static public boolean areTwoSortedDescriptionSetsSimilar(List<List<Encoder>> set1, List<List<Encoder>> set2,
					    MetricKind metric){
	float distance = 0;
	boolean setPenalty = false;
	try{
	    
	    int i = 0;
	    int lastStart = 0;
	    for (List<Encoder> path : set1){
		int numbersInPath = path.get(0).getNumberOfEncodings(); 
		i = lastStart;
		
		while(numbersInPath * (1 + THRESHOLD) < set2.get(i).get(0).getNumberOfEncodings())
		    i++;
		
		lastStart = i;

		for (; i<set2.size();i++) {
		    List<Encoder> elem = set2.get(i);
		    distance = hammingDistanceForTwoPaths(path, elem,setPenalty); 
		    int numbersInElem = elem.get(0).getNumberOfEncodings(); 
		    if(RELATIV){ // divide distance by number of coding elements
			//System.out.println("Absolute Distance " + distance);
			int size = numbersInPath;
			if(!setPenalty){
			    if(size > numbersInElem)
				size = numbersInElem;
			}else{
			    if(size < numbersInElem)
				size = numbersInElem;
			}    
			distance = distance/size;
			//System.out.println("Number  " + size);
			//System.out.println("Relative Distance " + distance);
			
		    }

		    if(distance <= THRESHOLD){
			//System.out.println("Path has been found. Distance is " + distance);
			//System.out.println("Original Path: "+ path);
			//System.out.println("Found Path: "+ elem);
			break;
		    }
	    
		    if(numbersInPath * (1 - THRESHOLD) >= numbersInElem || i+1 == set2.size()){
			//System.out.println("1 " + numbersInPath * (1 - THRESHOLD));
			//System.out.println("2 " +  numbersInElem);
			//System.out.println("break" + i + " " + set2.size());
			return false;
		    }
		    //System.out.println("Not found " + distance);
		    //System.out.println("Original Path: "+ path);
		    //System.out.println("Compared  Path: "+ elem);

		}
	    }
		
	}catch(Exception e){
	    e.printStackTrace();
	}
	    
	return true;
    
    }
    
    static boolean findPathWithHammingDistance(List<Encoder> path, List<List<Encoder>> set){
	float distance = 0;
	boolean setPenalty = false;

	try{
	    int numbersInPath = path.get(0).getNumberOfEncodings(); 
	    for (List<Encoder> elem : set) {

	        
		distance = hammingDistanceForTwoPaths(path,elem,setPenalty); 
		int numbersInElem = elem.get(0).getNumberOfEncodings(); 
	    	
		if(RELATIV){ // divide distance by number of coding elements
		    //System.out.println("Absolute Distance " + distance);
		    int size = numbersInPath;
		    if(!setPenalty){
			if(size > numbersInElem)
			    size = numbersInElem;
		    }else{
			if(size < numbersInElem)
			    size = numbersInElem;
		    }    
		    distance = distance/size;
		    //System.out.println("Number  " + size);
		    //System.out.println("Relative Distance " + distance);
		    
		}
		if(distance <= THRESHOLD){
		    //System.out.println("THRESHOLD " + THRESHOLD);
		    //System.out.println("Path has been found. Distance is " + distance);
		    //System.out.println("Original Path: "+ path);
		    //System.out.println("Found Path: "+ elem);
		    return true;
		}
		//System.out.println("Not found " + distance);
		//System.out.println("Original Path: "+ path);
		//System.out.println("Compared  Path: "+ elem);
		
	    }
	
	}
	catch(Exception e){
	    e.printStackTrace();
	}
	
	//System.out.println("Not found " + distance);
	return false;
    
    }

   
}

