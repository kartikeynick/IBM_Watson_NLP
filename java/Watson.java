import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.tartarus.snowball.ext.PorterStemmer;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.*;
import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import java.io.*;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Watson<Arraylist>
{
    //Instance variable
    static String fpi;//file path for index file
    static String index;//index location for File directory
    private static Provider BasicConfigurator;
    private static boolean isLem;//for lemmas
    private static  boolean isStem;//for stemming
    static String fpq;//file path for question file
    static String sf;//to store the scoring function (BM25 or cosine)
    static String alt;//analyzer Type which stores what type of analyzer (Standard, English or white)

    IndexWriter w;
    //Bonus
    boolean bonus=false,GlovDone=false;
    //Improving
    boolean improve=false;
    static boolean tuned=false;
    HashMap<String,ArrayList<Double>> ghm= new HashMap<String,ArrayList<Double>>();//Glove Hash Map

    Watson(String fpi,String fpq,String index,String sf,String alt,boolean isLem,boolean isStem)//constructor
    {
        Watson.fpi =fpi;
        Watson.fpq =fpq;
        Watson.index =index;
        Watson.alt =alt;
        Watson.sf =sf;
        Watson.isLem =isLem;
        Watson.isStem =isStem;
    }

    //cFunction to remove the stop words (Source: Github)
    public String removeSW(String n) throws IOException {
        //Storing it in an ArrayList
        List<String> data=new ArrayList<String>(Arrays.asList("a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"));

        String ar[]=n.split(" ");
        String ans="";
        for(String x:ar)
        {
            if(data.contains(x))
            {
                continue;// if found then continue
            }
            ans=ans+x+" ";
        }
        return ans;//returning the string with all the stopWords removed
    }

    public static String stem(String stem)//for stemming
    {
        //System.out.println("Stemming done");
        PorterStemmer stemmer = new PorterStemmer();
        stemmer.setCurrent(stem);
        stemmer.stem();
        return stemmer.getCurrent();//returned the string after stemming
    }
    public static String lemmas(String line)//for Lemmatizing
    {
        //System.out.println("lemmas are made");
        List<String> l = new LinkedList<String>();//linked list to store lemmas
        //using core NLP sentence for taking out lemmas/
        Sentence sentence = new Sentence(line);
        l = sentence.lemmas();
        String sb = "";
        //using string builder to convert array list into a string
        for (int x = 0; x < l.size(); x++) {
            sb = sb + l.get(x)+" ";
        }
        return sb;//returning the lemmas
    }

    public  void index() throws IOException//for indexing all the Wikipedia Documents
    {
        System.out.println("I am here at index and the wiki docs location is = "+fpi);

        Directory dir= FSDirectory.open(Paths.get(index));//using FSD instead of RAMD because the directory is big
        Analyzer anl;
        //choose the Analyzer (by default ill be English because it showed the best results)
        if(alt.equalsIgnoreCase("standard"))
        {
            anl=new StandardAnalyzer();
        }
        else if(alt.equalsIgnoreCase("english"))
        {
            anl=new EnglishAnalyzer();
        }
        else if(alt.equalsIgnoreCase("white"))
        {
            anl=new WhitespaceAnalyzer();
        }
        else
        {
            anl=new EnglishAnalyzer();//by default
        }
        IndexWriterConfig iwc=new IndexWriterConfig(anl);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);//watch here
        w=new IndexWriter(dir,iwc);

        //IndexDocs where getting all the doc present in the folder which contains the wiki docs and index them one by one
        File file=new File(fpi);
        File f[]=file.listFiles();
        for(int x=0;x<f.length;x++)
        {
            if(f[x].isFile())
            {
                //System.out.println(f[x]);//Printing the file to be indexed
                indexFile(w,f[x]);
            }
        }
        w.close();//Closing the index writer
    }
    public void indexFile(IndexWriter w,File f) throws IOException//Indexing the file here
    {
        System.out.println(f.getName()+" File is been indexing");//Printing the file name that is going to be indexed
        String fileAddr=f.toString();
        String title = "";//for title or docID
        String titlef="";//For the final/last title or docID
        String content="";//For the content that are in the page
        String c2= "";
        boolean a;
        boolean b=true;
        try
        {
            FileInputStream fm=new FileInputStream(fileAddr);//file
            //watch here
            Scanner sc=new Scanner(fm);//Scanner to read the lines
            String line;//to store the line
            while(sc.hasNextLine())//Till has next line
            {
                line=sc.nextLine();//stores the line, It will take in account the Categories as well
                //System.out.println("line = "+line);

                if(line.contains("#") || line.contains("=="))//Ignoring all the line that contains # or ==
                {
                    continue;//ignoring these lines
                }
                String t="\\[\\[(.+?)\\]\\]";// \\[\\[\\]\\]  [[]] -- getting all the page ID that are enclosed within [[text]]
                //Using Pattern matcher to match these
                Pattern pat = Pattern.compile(t); // eg: [[hi]]
                Matcher match=pat.matcher(line);

                if(match.find())
                {
                    c2="";
                    a=true;
                    titlef=match.group(1);//Goup 1 contains the text within [[ ]]
                    if(b)
                    {
                        b=false;
                        title=match.group(1);

                    }
                    else {
                        if(a) {
                            /*Tried it with extracting only the first 4 sentences and indexing it then removing the stopwords
                            String isbnArr[] = content.split("(?<=[a-z])\\.\\s+");
                            String isbn=(isbnArr.length>0?(isbnArr.length>4?isbnArr[0]+isbnArr[1]+isbnArr[2]+isbnArr[3]:isbnArr[0]):content);
                            String isbn=removeSW(content);*/

                            addDoc(w, title, content);//adding it into the document
                            title = match.group(1);//adding for the next title
                            content = "";//resetting the content
                        }
                    }
                }
                else
                {
                    //lemmas
                    if(isLem && line.length()>1) {
                        try {
                            String k = lemmas(line);
                            line = k;
                        }
                        catch (Exception e)
                        {
                        }
                    }
                    //stemming
                    if(isStem && line.length()>1)
                    {
                        String k=stem(line);
                        line =k;
                    }

                    content=content+line+" ";
                    c2=c2+line+" ";
                }
            }
            addDoc(w,titlef,c2);//index the last one

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    private static void addDoc(IndexWriter w, String title, String isbn) throws IOException//to add the docID and the isbn in the document object
    {
        Document doc = new Document();//Creating the document object to store the docID and isbn
        doc.add(new StringField("docid", title, Field.Store.YES));//storing the docID without tokenizing it
        doc.add(new TextField("isbn", isbn, Field.Store.YES));// storing the rest of the data used to search but tokenizing it
        w.addDocument(doc);

    }

    public  void SearchIndexDocs() throws IOException, ParseException//For searching  the Indexed doc in Lucene
    {
        System.out.println("Lets do Search index docs ");
        String que = "";//For question
        boolean a = false;
        String res = "", ans = "";//For result and answer
        int CHits = 0, tc = 0;//hit counts and total count
        //w.close();//closing the writer before opening the directory to handle deadLock
        FSDirectory dir = FSDirectory.open(Paths.get(index));//FSD
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher is = new IndexSearcher(reader);

        //BM25 and cosine similarity

        if (sf.equalsIgnoreCase("BM25"))//by default it is BM25
        {
            if (tuned) {
                is.setSimilarity(new BM25Similarity(1.5f, 0.2f));//tuning the similarity in 3-34% and 0.433
            } else {
                is.setSimilarity(new BM25Similarity());//changing the similarity ti BM25 (however it is by default)
            }
        } else if (sf.equalsIgnoreCase("cosine"))
        {
            is.setSimilarity(new ClassicSimilarity());//changing the similarity to cosine
        }


        Analyzer anl;//Choosing the Analyzer from the input
        if (alt.equalsIgnoreCase("Standard")) {
            anl = new StandardAnalyzer();
        } else if (alt.equalsIgnoreCase("English")) {
            anl = new EnglishAnalyzer();
        } else if (alt.equalsIgnoreCase("White")) {
            anl = new WhitespaceAnalyzer();
        } else {
            anl = new StandardAnalyzer();//by default it will be standard analyzer as it shows the best results
        }

        File fl = new File(fpq);//getting the question file
        Scanner sc = new Scanner(fl);//Scanner to read file

        int ln = 0;//line number for navigating through lines
        String catagory = "";//category for the category in the file
        double ancs = 0.0, total = 0.0;// for calculating the ranks
        double rank = 0.0;//for MRR
        double vecP1 = 0.0, vecM = 0.0;
        double tfP1=0.0,tfM=0.0;//term frequency p1 rank, term frequency mean reciprocal rank
        while (sc.hasNextLine()) {
            try {
                /*
                The position of these it in the question file goes like this:
                -->Category
                -->Question
                -->Answer
                /s
                So reading line one by one
                 */
                catagory = sc.nextLine();
                que = sc.nextLine();
                ans = sc.nextLine();

                sc.nextLine();//to get rid of the blank line
                if (isLem && que.length() > 1)//for stemming
                {
                    String temp = lemmas(que);//temp String
                    que = temp;
                }
                //Stemming
                if (isStem && que.length() > 1)//for Lemmas
                {
                    String temp = stem(que);//Temp String

                    que = temp;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            que = que + " " + catagory;//adding the category to the question
            que = que.replaceAll("\\r\\n", " ");
            

            Query query = new QueryParser("isbn", anl).parse(QueryParser.escape(que));//Query Parser to parse the Question to "isbn" portion of the index for searching

            TopDocs tdc = is.search(query, 20);//index searcher
            ScoreDoc hits[] = tdc.scoreDocs;//this is the score of wiki docs
            //System.out.println("Score array len = "+hits.length);


            //Implementing the Bonus Question using GloVe

            if (bonus)//If bonus == true
            {
                //Implementing the bonus one
                TreeMap<Double, String> newS = new TreeMap<Double, String>();//Storing it in Tre map so it get sorted
                //Making the vectors for the que
                que=removeSW(que);//removing all the stop words form the Que file
                ArrayList<Double> queD = new ArrayList<Double>();
                queD = glove(que);//finding out all the vectors from the GloVe file

                //Making the vectors for the top 10 hits in the page
                for (ScoreDoc x : hits) {
                    ArrayList<Double> wikiD = new ArrayList<Double>();
                    Document ansdc = is.doc(x.doc);
                    //String isbn=ansdc.get("isbn");
                    String isbnArr[] = ansdc.get("isbn").split("(?<=[a-z])\\.\\s+");//Seplitting by the sentences
                    String isbn=(isbnArr.length>0?(isbnArr.length>4?isbnArr[0]+isbnArr[1]+isbnArr[2]+isbnArr[3]:isbnArr[0]):ansdc.get("isbn"));//Taking only the first two sentences if exists otherwise only the first sentence
                    isbn = removeSW(isbn);//removing all the stop words
                    //System.out.println("GloVe for isbn = " + isbn);//Debug
                    wikiD = glove(isbn);//finding out all the vectors
                    double cosS = cosineSimilarity(queD, wikiD);//finding the cosine similarity in between the two vectors of que and wiki page
                    //System.out.println(" the Cosine Similarity = " + cosS);
                    String docid = ansdc.get("docid");
                    newS.put(cosS, docid);
                }
                double dr1 = 0.0;//Finding the reranked Results
                for (String x : newS.values()) {
                    dr1++;
                    int c=0;
                    if (ans.equalsIgnoreCase(x)) {
                        //System.out.println("Answer\n"+ans + "\n\n\nmathed with " + x + "\n\n\tThese score = "+dr1);
                        if(dr1==1) {
                            vecP1++;//vector p@1
                        }
                        break;
                    }
                }
                vecM = (1.0 / dr1) + vecM;
            }
            //Bonus code ends (Later has the score calculations and the methods that are called)


            //Improve rank by term frequencies Q5
            //To improve ranking
            double dr2=0.0;
            /* vocab list
            have to do for both the Query and the document
            tf-raw= is the number of times the word come in vocab (do both for doc)
            tf-weight= log(tf-raw)+1 // if 0 the 0
            df= number of times each word in the vocab come in the document
            idf= inverse document freq(N/log(df))
            weight = tf*idf
            for document all same but tf-weight=weight
            n'lized= a=Math.sqrt(SumOf(All the weight in the document)); the weight/a= nlized
            *
            rank= weight*n'lized// is the rank
            * */
            if(improve) {

                TreeMap<Double,String> tm=new TreeMap<Double,String>();
                HashMap<String,Double> queryDf=new HashMap<String,Double>();
                ArrayList<String> temp=new ArrayList<>();
                for(ScoreDoc x:hits)
                {
                    Document ansdc = is.doc(x.doc);
                    //String isbn=ansdc.get("isbn");
                    String isbnArr[] = ansdc.get("isbn").split("(?<=[a-z])\\.\\s+");//Seplitting by the sentences
                    String isbn=(isbnArr.length>0?(isbnArr.length>4?isbnArr[0]+isbnArr[1]+isbnArr[2]+isbnArr[3]:isbnArr[0]):ansdc.get("isbn"));//Taking only the first two sentences if exists otherwise only the first sentence
                    String isbnArr2[] = removeSW(isbn).split(" ");

                    for (String sl:isbnArr2)
                    {
                        temp.add(sl);
                    }
                }
                for(ScoreDoc x:hits) {
                    ArrayList <String> vocab=new ArrayList<String>();

                    Document ansdc = is.doc(x.doc);
                    String isbn=ansdc.get("isbn");
                    //String isbnArr[] = ansdc.get("isbn").split("(?<=[a-z])\\.\\s+");//Seplitting by the sentences
                    //String isbn=(isbnArr.length>0?(isbnArr.length>4?isbnArr[0]+isbnArr[1]+isbnArr[2]+isbnArr[3]:isbnArr[0]):ansdc.get("isbn"));//Taking only the first two sentences if exists otherwise only the first sentence
                    isbn = removeSW(isbn);//removing all the stop words
                    //create a vocab
                    String s[]=(que+isbn).split(" ");
                    for(String x1:s)
                    {
                        if(!(vocab.contains(x1)))
                        {
                            vocab.add(x1);
                        }
                    }
                    ArrayList<Double> wtq=new ArrayList<Double>();
                    ArrayList<Double> wtd=new ArrayList<Double>();
                    double sum=0.0;
                    String qarr[]=que.split(" ");
                    String darr[]=isbn.split(" ");
                    for(String a1:vocab)
                    {
                        double tf=0.0,tf2=0.0;
                        for(String b:qarr)
                        {
                            if(a1.equalsIgnoreCase(b))
                            {
                                tf++;
                            }
                        }
                        for(String b1:darr)
                        {
                            if(a1.equalsIgnoreCase(b1))
                            {
                                tf2++;
                            }
                        }
                        tf=tf>0?(Math.log(tf)+1):0;//tf
                        tf2=tf2>0?(Math.log(tf2)+1):0;
                        sum=tf2+sum;
                        wtd.add(tf2);
                        int df=Collections.frequency(temp, a1);
                        double idf=20/Math.log(df);
                        wtq.add(tf*idf);
                    }
                    double te=Math.sqrt(sum);
                    double ranktfidf=0.0;
                    for(int i=0;i<wtd.size();i++)
                    {
                        ranktfidf=(wtd.get(i)/te)+wtq.get(i);
                    }

                    tm.put(ranktfidf,(ansdc.get("docid")));
                }
                for (String x : tm.values()) {
                    dr2++;
                    int c=0;
                    if (ans.equalsIgnoreCase(x)) {
                        //System.out.println("Answer\n"+ans + "\n\n\nmathed with " + x + "\n\n\tThese score = "+dr2);
                        if(dr2==1) {
                            tfP1++;//vector p@1
                        }
                        break;
                    }
                }
                tfM = (1.0 / dr2) + tfM;
            }
            //Ends here

            //Start of normal bm25 retrieval using Lucene
            double dr = 0.0;//The rank of where it matches

            //
            for (ScoreDoc x : hits)//for all the hits
            {
                dr++;
                Document ansdc = is.doc(x.doc);

                if (ans.equalsIgnoreCase(ansdc.get("docid")))//check if the matched que is same as the docId
                {
                    //System.out.println("\n\tMatched = " + (ansdc.get("docid")) + "\n\tWith Answer " + ans + "\nRanking  = " + dr);
                    if(dr==1) {
                        ancs++;//answer counter for P@1
                    }
                    break;//break the loop if found
                }
                if(dr<6)
                {
                    String isbnArr[] = ansdc.get("isbn").split("(?<=[a-z])\\.\\s+");//Seplitting by the sentences
                    String isbn=(isbnArr.length>0?(isbnArr.length>4?isbnArr[0]+"\n"+isbnArr[1]+"\n"+isbnArr[2]+"\n"+isbnArr[3]:isbnArr[0]):ansdc.get("isbn"));//Taking only the first two sentences if exists otherwise only the first sentence

                }
            }
            rank = (1.0 / dr) + rank;//MRR
            total++;
        }
        //Normal without GloVe
        double mrr = rank / total;//MRR
        double ttl = (ancs / total) * 100;//P@1

        System.out.println("For This Retrieval\nMean Reciprocal Rank = " + mrr);
        if (bonus) {
            mrr = (vecM / total); //Vect MMR
            ttl = (vecP1 / total) * 100;//P@1
            //With GloVe
            System.out.println("For This Retrieval\nMean Reciprocal Rank = " + mrr);
        }
        if(improve)
        {
            mrr = (tfM / total); //Vect MMR
            ttl = (tfP1 / total) * 100;//P@1
            //With GloVe
            System.out.println("For This Retrieval\nMean Reciprocal Rank = " + mrr);
        }
    }
    public double count(String que,String isbn)
    {
        double d=0.0;
        String ar1[]=que.split(" ");//splitting questions by space
        String ar2[]=isbn.split(" ");//splitting isbn by space
        for(String x:ar1)
        {
            double c=0;
            for(String y:ar2)
            {
                if(y.equalsIgnoreCase(x))
                {
                    c++;
                }
            }
            d=d+c;
        }
        return d;
    }

    public double cosineSimilarity(ArrayList<Double> ar1, ArrayList<Double> ar2)// finding the cosine similarity of two arrayList
    {
        double ans=0.0;//The answer to return
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int l1=ar1.size();
        int l2=ar2.size();
        int min=Math.min(l1,l2);
        //Implementing the CosuneSimilarity Formula
        for (int i = 0; i < min; i++) {
            dotProduct += (ar1.get(i)) * (ar2.get(i));
            normA += Math.pow(ar1.get(i), 2);//Taking the power of 2
            normB += Math.pow(ar2.get(i), 2);
        }
        ans= dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));//finding the cosine similarity in between 2 ArrayList of doubles
        return ans;//returning the result
    }


    /* Getting a OutOfMemoryError: Java heap space while loading it on the memory*/
    public  void glovehm(String f) throws FileNotFoundException//loading the GloVe file in main memory in a HashMap ghm-GloVe Hash Map
    {
        File file=new File(f);
        Scanner sc=new Scanner(file);

        while (sc.hasNextLine())
        {
            String s[] = sc.nextLine().split(" ");//Splitting by space
            ArrayList<Double> d=new ArrayList<Double>();//to store the float numbers in an arrayList to store it in a hash map

            for(int x=1;x<s.length;x++)
            {
                d.add(Double.parseDouble(s[x]));//converting string into double and adding it into the arrayList
            }
            ghm.put(s[0],d);//put the first word and the rest of the floating point in the ghm(glove hash map)
        }
    }

    public ArrayList<Double> glove(String n) throws FileNotFoundException {

        if(GlovDone)//stores the GloVe file in a TreeMap<String,<ArrayList<Double>> and load it in main memory
        {
            GlovDone=false;
            glovehm("src\\main\\resources\\glove.txt");
        }

        /*ghm for loading it into RAM

        String s[]=n.split(" ");
        ArrayList<Double> d=new ArrayList<Double>();
        for(String x:s)
        {
            if(ghm.containsKey(x))
            {
                d=ghm.get(x);
            }
        }*/

        //Loading it from the disk every time it gets called and the comparing it from the GloVe file
        Scanner sc=new Scanner(new File("src\\main\\resources\\glove.txt"));//location of the glove file
        ArrayList<Double> d=new ArrayList<Double>();//array return
        String ar[]=n.split(" ");//for the isbn returned by lucene
        int l=ar.length;
        int f=0;
        String g1[]= sc.nextLine().split(" ");
            while(sc.hasNextLine())
            {
                f++;
                String s[] = sc.nextLine().split(" ");//for each word in GloVe file
                int found=0;
                for(int x=0;x<ar.length;x++)
                {
                    if(s[0].equalsIgnoreCase(ar[x]))//glove word equal to the isbn
                    {
                        found=1;
                        for(int y=1;y<s.length;y++)//store the rest in d
                        {
                            d.add(Double.parseDouble(s[y]));//storing the rest by converting it in double
                        }
                         break;//break from for
                    }
                }
                if(found==0)
                {
                    for(int y=1;y<g1.length;y++)//store the rest in d
                    {
                        d.add(Double.parseDouble(g1[y]));//storing the rest by converting it in double
                    }
                }
                if(f==l)
                {
                    break;
                }
            }
        return d;
        }


    public static void main(String args[]) throws IOException, ParseException, InterruptedException {
        //BasicConfigurator.configure();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("\nProject for Information Retrieval \n-By Kartikey Shukla\n");
        String wiki = "src\\main\\resources\\WikiDocs";//Location of the WikiDocs
        String que = "src\\main\\resources\\questions.txt"/*Location of the Question*/, config = "", scoringFun = "", anlz = "";//Config for best config, scoring function and analyzer
        String index = "src\\main\\resources\\index";
        boolean lem = false, stem = false, bon=false,improve1=false;//Boolean for lemmas and stemming

        //choose the analyser then rhe scoring function
        int ch;
        /*
         * 1-Best
         * 2-Modify
         *   a-Choose the Analyzer
         *   b-Choose the scoring Function
         *   c-Want to use Lemmas?
         *   d-Want to use stemming?
         * 0-Exit*/
        do {
            System.out.println("Press 1 to run the best & improved config\nPress 2 to customize Scoring function, Analyzer, choose if you want to take lemmas and do stemming or not\nPress 3 for Bonus round\nPress 4 for bonus round\nPress 0 to Exit");
            System.out.println("Make your selection");
            ch = Integer.parseInt(br.readLine());
            int flag = 0;
            switch (ch) {
                case 0:
                    System.out.println("Thank you for using this IR system :) and Have a nice Day");
                    break;
                case 1:// The best config
                    System.out.println("Here is the best cofig:\nAnalyzer is English and the Similarity is BM25 (Improved: Tuned) ");
                    config = "best";
                    break;
                case 2:

                    System.out.println("Choose the Analyzer:Press\n1- Standard\n2- English\n3- WhiteSpace");
                    int ch2 = Integer.parseInt(br.readLine());
                    if (ch2 == 1) {
                        anlz = "Standard";
                    } else if (ch2 == 2) {
                        anlz = "English";
                    } else if (ch2 == 3) {
                        anlz = "WhieSpace";
                    } else {
                        System.out.println("Invalid Input, kindly try again");
                        flag = 1;
                        break;
                    }
                    System.out.println("Now choose the scoring function: Press\n1- BM25\n2- Cosine");
                    int ch3 = Integer.parseInt(br.readLine());
                    if (ch3 == 1) {
                        scoringFun = "BM25";
                    } else if (ch2 == 2) {
                        scoringFun = "Cosine";
                    } else {
                        System.out.println("Invalid Input, kindly try again");
                        flag = 1;
                        break;
                    }

                    System.out.println("Want to take out Lemmas as well? if yes then press 1 else 0");
                    int ch4 = Integer.parseInt(br.readLine());
                    lem = ch4 == 1;//lem=ch4==1?true:false;
                    System.out.println("Want to take out Stemming? if yes then press 1 else 0");
                    int ch5 = Integer.parseInt(br.readLine());
                    stem = ch5 == 1 ? true : false;
                    break;
                case 3:
                    bon=true;
                    break;
                case 4:
                    improve1=true;
                default:
                    System.out.println("The input was invalid, Kindly try again");
                    break;
            }
            if (flag == 1) {
                break;
            } else if (config.equalsIgnoreCase("best")) {

                Watson ob = new Watson(wiki, que, index, "bm25", "english", false, false);
                ob.tuned=true;
                ob.index();
                ob.SearchIndexDocs();
                config = "";
            }
            else if(improve1)
            {
                Watson ob = new Watson(wiki, que, index, "bm25", "english", false, false);
                System.out.println("Now Lets start:\nPick 1-for BM25 Tuned and\nPick 2- For TFIDF");
                int impo = Integer.parseInt(br.readLine());
                if (impo == 1) {
                    ob.tuned = true;
                } else if (impo == 2) {
                    ob.improve = true;
                } else {
                    System.out.println("Invalid input, please try again");
                }
                ob.index();
                ob.SearchIndexDocs();
            }
            else if(bon)
            {
                Watson ob = new Watson(wiki, que, index, "bm25", "english", false, false);
                ob.bonus=true;
                ob.index();
                ob.SearchIndexDocs();
                config = "";
            }
            else {
                System.out.println("Now Lets start");
                Watson ob = new Watson(wiki, que, index, scoringFun, anlz, lem, stem);
                ob.index();
                ob.SearchIndexDocs();
            }
        } while (ch != 0);
    }
 }
