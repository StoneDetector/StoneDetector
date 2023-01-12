package org.fsu.codeclones;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class ModifiedHammingDistance{

    static float THRESHOLD = (float) 0;
    static boolean RELATIV = true;
    static boolean SORTED = false;
	

    static float modifiedHammingDistance(int[] a1, int[] a2){

	if(a1[0] != a2[0]) //check kind statement
	    return 1;
	
	if(a1.length==1 && a2.length==1){
	    return 0;
	}
	
	float distance = 0;
	int number = a1.length - 1;
	int size = a1.length;
	//Determine lower size and set the initial value of distance
	if(size > a2.length){
	    distance += size - a2.length;
	    size = a2.length;
	    
	} else{
	    if(size < a2.length){
		distance += a2.length - size;
		number = a2.length - 1;
	    }
	}
	//Determine the distance of operators
	for(int i = 1; i<size;i++)
	    if(a1[i] != a2[i])
		distance++;
	//return a relative value <= 0.5
	//System.out.println("Hamming" + (distance/number));
	return distance/number;// /2.0F;
    }

     static float modifiedHammingDistanceForTwoPathsUnsorted(List<Encoder> l, List<Encoder> k, boolean sizePenalty) {
	 //System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
	if(l == null || k == null)
	    throw new IllegalArgumentException();
	List<Encoder> sl = l;
	List<Encoder> ll = k;
	float distance = 0;
	float tmp, tmp1;

	if(l.size() > k.size()){
	    sl = k;
	    ll = l;
	}

	int i = 0, j = 0;
       
	
	for (; i < sl.size(); i++) {	    
	    
	    Encoder slElement = sl.get(i);
	    j = i;
	    tmp = 1;
	    int number = -1;
	    for(; j< ll.size(); j++) {
		    Encoder llElement =ll.get(j);
		    tmp1= modifiedHammingDistance(slElement.getEncoding(), llElement.getEncoding());
		    //System.out.println("1 " + sl.get(i));
		    //System.out.println("2" + ll.get(j));
		    //System.out.println("Tmp1 :" +tmp1);
		    //System.out.println("Tmp :" + tmp);

		    if(tmp1 < tmp)
			tmp = tmp1;
		       
		    if(tmp == 0.0F)
			break;
        
		    //System.out.println("tmp1 " + tmp1);
		    //System.out.println("tmp " + tmp);
		    //}
       
            }
	    
		distance += tmp;

	    //System.out.println("Distance: " + distance);

	}
	if(sizePenalty)    
	    for (; i < ll.size(); i++){ 
		distance++;
		//System.out.println(distance);  
	    }
	//if(distance > 1)
	//System.out.println(distance);
	return distance;
    }
    
  static float modifiedHammingDistanceForTwoPathsSorted(List<Encoder> l, List<Encoder> k, boolean sizePenalty) {
      //System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
	if(l == null || k == null)
	    throw new IllegalArgumentException();
	List<Encoder> sl = l;
	List<Encoder> ll = k;
	float distance = 0;
	float tmp, tmp1;

	if(l.size() > k.size()){
	    sl = k;
	    ll = l;
	}

	int j=0;
	int i=0;
	
	int lastStart = 0;
		
	for (; i < sl.size(); i++) {	    
	    
	    Encoder slElement = sl.get(i);
	    j = lastStart;

	    while(j < ll.size() && slElement.getKind().ordinal() < ll.get(j).getKind().ordinal() )
		j++;

	    lastStart = j;
	    tmp = 1;
	    int number = -1;
	    for(; j< ll.size() && slElement.getKind() == ll.get(j).getKind(); j++) {
		    Encoder llElement =ll.get(j);
		    tmp1= modifiedHammingDistance(slElement.getEncoding(), llElement.getEncoding());
		    //System.out.println("1 " + sl.get(i));
		    //System.out.println("2" + ll.get(j));
		    //System.out.println("Tmp1 :" +tmp1);
		    //System.out.println("Tmp :" + tmp);

		    if(tmp1 < tmp){
			tmp = tmp1;
			number = j;
		    }
		    if(tmp == 0.0F)
			break;
        
		    //System.out.println("tmp1 " + tmp1);
		    //System.out.println("tmp " + tmp);
		    //}
       
            }
	    
		distance += tmp;

	    //System.out.println("Distance: " + distance);

	}
	
	if(sizePenalty)    
	    for (; i < ll.size(); i++){ 
		distance++;
		//System.out.println(distance);  
	    }
	//if(distance > 1)
	//System.out.println(distance);
	return distance;
    }
    
    static float findPathWithModifiedHammingDistance(List<Encoder> path, List<List<Encoder>> set){
	float distance = 0;
	boolean setPenalty = true;
	int nodesInPath = path.size();
	//System.out.println(" Path: "+ path);
	//System.out.println("Set: "+ set);
	float medium = 1.0F;
	
	for (List<Encoder> elem : set) {
	    int nodesInElem = elem.size();
	    int size1 = nodesInPath;
	    int size2 = nodesInElem;
	    
	    if(	nodesInPath >  nodesInElem){
		   size2 = nodesInPath;
		   size1 = nodesInElem;
	    }

	    if((size2 - size1) < (size1 * 0.6)){
		if(SORTED)
		    distance = modifiedHammingDistanceForTwoPathsSorted(path,elem,setPenalty);
		else
		    distance = modifiedHammingDistanceForTwoPathsUnsorted(path,elem,setPenalty);
	    } else
		continue;
		
	   
	    int size = nodesInPath;

	    if(!setPenalty){
		if(size > nodesInElem)
		    size = nodesInElem;
	    }else{
		if(size < nodesInElem)
		    size = nodesInElem;
	    }
	    
	    if(distance/size < medium)
		medium = distance/size;
	
	}
	
	return medium;
    }
    
    // Sorted

    static boolean findPathWithModifiedHammingDistanceInSorted(List<Encoder> path, List<List<Encoder>> set){
	float distance = 0;
	boolean setPenalty = false;

	try{
	    int numbersInPath = path.get(0).getNumberOfEncodings(); 
	    for (List<Encoder> elem : set) {
		if(numbersInPath * (1 + THRESHOLD) > elem.get(0).getNumberOfEncodings())
		    continue;
		distance = modifiedHammingDistanceForTwoPathsSorted(path,elem,setPenalty);
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
		    //System.out.println("Relative Distance" + distance);
		    
		}

		if(distance <= THRESHOLD){
		    //System.out.println("Path has been found. Distance is" + distance);
		    //System.out.println("Original Path: "+ path);
		    //System.out.println("Found Path: "+ elem);		
		    return true;
		}//else
		//System.out.println("Path has not been found. Distance is" + distance);
		if(numbersInPath * (1 - THRESHOLD) < elem.get(0).getNumberOfEncodings())
		    break;	
	    }
	}catch(Exception e){
	    e.printStackTrace();
	}
	
	return false;
    }

    
}

