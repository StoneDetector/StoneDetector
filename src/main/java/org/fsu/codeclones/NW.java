package org.fsu.codeclones;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;


public class NW {

    private static final double THRESHOLD_NW_AVG = 0.3;
    private static final double THRESHOLD_HAMMING = 0.2;

    public static boolean compareSets(List<List<Encoder>> set1, List<List<Encoder>> set2) {

        //List<List<Encoder>> tmpSet1 = new ArrayList<>(set1);
        //List<List<Encoder>> tmpSet2 = new ArrayList<>(set2);

        double finalScore = 0.0;

        for (List<Encoder> path1 : set1) {
	    //System.out.println("Q");
            double min = 1;
            for (List<Encoder> path2 : set2) {
                double nw = score(path1, path2);
                int size1 = path1.size();
                int size2 = path2.size();
                double score1 = (size1-nw)/size1;
                double score2 = (size2-nw)/size2;
                double score = (score1 + score2) / 2.0;

                if (score < min) {
                    min = score;
                    //maxIdx = idx;
                }
                //idx++;
            }

            //if (maxIdx >= 0) {
                //tmpSet2.remove(maxIdx);
	    finalScore += min;
		//}
	    if((finalScore/set1.size()) > THRESHOLD_NW_AVG)
		return false;
        }

        finalScore /= set1.size();

        return finalScore <= THRESHOLD_NW_AVG;
    }
    public static double score(List<Encoder> strand1, List<Encoder> strand2) {
        return new NW.InternalNw(strand1, strand2, 1, -1, 0).getScore();
    }

    static class InternalNw {

        private final int match;
        private final int mismatch;
        private final int gap;

        private final List<Encoder> strand1;
        private final List<Encoder> strand2;

        private int score;

        public InternalNw(List<Encoder> strand1, List<Encoder> strand2, int match, int mismatch, int gap) {
            this.strand1 = strand1;
            this.strand2 = strand2;

            this.match = match;
            this.mismatch = mismatch;
            this.gap = gap;

            calculate();
        }

        public int getScore() {
            return score;
        }

        private void calculate() {
            final int n1 = strand1.size();
            final int n2 = strand2.size();

            final int[][] matrix = new int[n1 + 1][n2 + 1];
            matrix[0][0] = 0;

            for (int i = 1; i < n1 + 1; i++) {
                matrix[i][0] = matrix[i-1][0] + gap;
            }

            for (int j = 1; j < n2 + 1; j++) {
                matrix[0][j] = matrix[0][j-1] + gap;
            }

            for (int i = 1; i < n1 + 1; i++) {
                for (int j = 1; j < n2 + 1; j++) {

                    int matchMisScore = mismatch;

		    //   if (ModifiedHammingDistance.modifiedHammingDistance(strand1.get(i-1).getEncoding(),
		    if (compare(strand1.get(i-1).getEncoding(),
				strand2.get(j-1).getEncoding()) == 0F) {
                        matchMisScore = match;
                    }

                    matrix[i][j] = max(matrix[i - 1][j] + gap, matrix[i][j - 1] + gap,
                            matrix[i - 1][j - 1] + matchMisScore);
                }
            }

            score = matrix[n1][n2];
        }

        private static int max(int a, int b, int c) {
            return Math.max(Math.max(a, b), c);
        }

	private static final float compare(int[] a1, int[] a2){

	    if(a1.length != a2.length)
		return 1;
	    
	    for(int i = 0; i < a1.length; i++)
		if(a1[i] != a2[i])
		    return 1;
	    
	    return 0;
	}	  
    }
}
