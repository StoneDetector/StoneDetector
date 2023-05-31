package org.fsu.codeclones;

public abstract class Environment {

    //Size of used Threadpool
    public static Integer THREADSIZE=1;


    // Metric kind: MetricKind.LCS, MetricKind.LEVENSHTEIN, METRICKind.MODIFIEDHAMMING, MetricKind.HAMMING, MetricKind.EUCLIDEAN MetricKind.NW

    public static MetricKind METRIC = MetricKind.LCS;

    // paths in description sets: EncoderKind.SPLITTING, EncoderKind.UNSPLITTING

    public static EncoderKind PATHSINSETS = EncoderKind.UNSPLITTING;


    //Kind of approach: EncodeKind.HASH EncoderKind.ABSTRACT EncoderKind.COMPLETEPATH EncoderKind.MULTISET

    public static EncoderKind TECHNIQUE = EncoderKind.HASH;

    //Kind of ordering in description sets: MetricKind.UNSORTED MetricKind.SORTED

    public static EncoderKind SETORDER = EncoderKind.UNSORTED;

    public static float THRESHOLD = 0.3F;

    // Default value: LCS: 0.3 Levenshtein: 0.35


    // mininml size of tested Funktions
    public static int MINSIZE = 15;

    // suoprting Call names
    public static boolean SUPPORTCALLNAMES = true;

    //supporting linear subclones

    public static boolean LINEARSUBCLONES = false;

    // linear factor default Unsplitting 1.4
    public static double LINEARFACTOR = 1.4;
    public static int MINLINEARSUBCLONE = 5;

    // max no. of paths in description sets (for calculations of upper paths)
    public static int WIDTHLOWERNO = 5;
    // Default value: 5

    // faktor for the  no. of upper paths in description sets
    public static float WIDTHUPPERFAKTOR = 1.5F;
    // Default value: 1.5

    // min number of nodes in paths of description sets
    public static int MINNODESNO=3;

    //Default values: Modified-Unsorted Splitting: 1 Modified-Unsorted Unsplitting 3, LCS Splitting 1 LCS Unsplitting 3 Levenshtein Spliiting  1, Levenshtein Unsplitting 3

    //max difference when comparing path sets: default value 7
    public static int MAXDIFFNODESNO = 7;

    // relative metrics
    public static boolean RELATIVE = true;

    // sorted description sets
    public static boolean SORTED = false;


    // output of clone pairs

    public static boolean OUTPUT = true;

    // set MD5 hashing

    public static boolean MD5 = false;


    public static boolean BYTECODEBASED=true;

    //for ByteCode
    public static float BREMOVESMALLPATHES =0.3f;
    public static float BPATHESDIFF =0.05f;

    //for RegisterCode
    //float BREMOVESMALLPATHES =0.4f;
    //float BPATHESDIFF =0.3f;

    public static boolean USEREGISTERCODE=false;

    public static boolean STUBBERPROCESSING=false;
}
