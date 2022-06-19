package org.fsu.codeclones;

import java.util.List;

public class EuclideanDistance{
    static double THRESHOLD = 0;

    public static double modifiedHamming(int a[], int b[]){
	double result=0;
	
	for(int i=0; i<a.length; i++)
	    if(a[i] > b[i])
		result = result + a[i]-b[i];
	    else
		result = result + b[i]-a[i];
	return result;
    }
    
    static boolean findPathWithModifiedHamming(List<Encoder> path, List<List<Encoder>> set){
	double distance = 0;
	int number =  ((AbstractEncoder)path.get(0)).getNumber();
	for (List<Encoder> elem : set) {
	    if(number <  ((AbstractEncoder) elem.get(0)).getNumber())
		number =  ((AbstractEncoder)elem.get(0)).getNumber();
	    distance = modifiedHamming(path.get(0).getEncoding(), elem.get(0).getEncoding());
	    distance /= number;
	    
	    //System.out.println(THRESHOLD);
	    if(distance <= THRESHOLD){
		//System.out.println("Path has been found. Distance is " + distance);
		//System.out.println("Original Path: "+ path.get(0));
		//System.out.println("Found Path: "+ elem.get(0));
		return true;
	    }

	}
	//System.out.println("Path not has been found. Distance is " + distance);
	return false;
    
    }
    
    static double findPathWithEuclideanDistance(List<Encoder> path, List<List<Encoder>> set){
	double distance = 0;
	double medium = 100;
	int numberInPath = ((AbstractEncoder)path.get(0)).getNumber();
	int numberInElem;

	for (List<Encoder> elem : set) {
	    //System.out.println("Path " + path.get(0));
	    //System.out.println("Elem " + elem.get(0));
	    numberInElem = ((AbstractEncoder) elem.get(0)).getNumber();
	    int number = numberInPath;  
	    if(Math.abs(numberInPath - numberInElem) < 7){
		if(number <  numberInElem) 
		    number =  numberInElem;

		distance = distance(path.get(0).getEncoding(), elem.get(0).getEncoding());

		//System.out.println("Distance is " + distance);

		if(distance/number < medium)
		    medium = distance/number;

	    }
	}
	
	//System.out.println("Medium " + medium );
	return medium;
    
    }
    

	/*
	*euclidean scalar product
	*/
    public static double euklideanSkalarProduct(double[] a, double[]b){
		double result=0;
		for(int i=0; i<a.length; i++)
		    result = result + a[i]*b[i];
		return result;
	}

	/*
	*euclidean Norm of a vectors
	*/
	public static double euklideanNorm(double [] a){
		return Math.sqrt(euklideanSkalarProduct(a,a));
	}

	
	
	/*
	*euclidean distance
	*/
	public static double distance(int [] a, int [] b){
	    double [] cp = new double [a.length];
	    //System.out.println(a.length + " " + b.length);
	    for(int i=0;i<cp.length;i++){
			cp[i]=a[i]-b[i];
			//System.out.println(a[i]);
			//System.out.println(b[i]);			
			//System.out.println(cp[i]);
	    }
		
		return euklideanNorm(cp);
	}
		

}
