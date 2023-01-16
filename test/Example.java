import java.util.Random;

public class Main {
    int[][] fillMatrixRandom1(int matrix[][])
    {
        Random random = new Random(1);

        for (int i = 0; i< matrix.length;i++){

            for (int j=0;j<matrix[i].length;j++)
            {
                matrix[i][j]= random.nextInt();

            }

        }
        return matrix;
    }
    int[][] fillMatrixRandom2(int matrix[][])
    {
        Random random = new Random(1);
        int i = 0;
        while (i< matrix.length){
            int j=0;
            while(j<matrix[i].length)
            {
                matrix[i][j]= random.nextInt();
                j++;
            }
            i++;
        }
        return matrix;
    }
}