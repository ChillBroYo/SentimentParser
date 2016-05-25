import org.apache.lucene.document.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//import org.apache.lucene.queryParser.QueryParser;

/*
 * (Really simple-dumb) Sentiment analysis for a lucene index of 1 million Tweets!
 * Based on http://jeffreybreen.wordpress.com/2011/07/04/twitter-text-mining-r-slides/
 *
 */
public class Analyze {

    // path to lucene index
    //private final static String inputFile = "SampleTweetText/sampleInput.txt";
    // path to language profiles for classifier

    // lucene queryParser for saving
    //private static QueryParser queryParser;

    // used to store positive and negative words for scoring
    static List<String> posWords = new ArrayList<String>();
    static List<String> negWords = new ArrayList<String>();

    // keep some stats! [-1 / 0 / 1 / not english / foursquare / no text to
    // classify]
    static int[] stats = new int[6];

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        boolean quiet = false;
        for (String arg : args)
        {
            if (arg.equals("-q"))
            {
                quiet = true;
            }
        }

        // huh, how long?
        long startTime = System.currentTimeMillis();

        // open lucene index
        BufferedReader reader = null;
        try {
            //FileReader fr = new FileReader(inputFile);
            InputStreamReader isr = new InputStreamReader(System.in);
            reader = new BufferedReader(isr);

            System.out.println("START: reading file list");
            // source: www.cs.uic.edu/~liub/FBS/sentiment-analysis.html
            BufferedReader negReader = new BufferedReader(new FileReader(new File(
                    "negative-words.txt")));
            BufferedReader posReader = new BufferedReader(new FileReader(new File(
                    "positive-words.txt")));

            // currently read word
            String word;

            // add words to comparison list
            while ((word = negReader.readLine()) != null) {
                negWords.add(word);
            }
            while ((word = posReader.readLine()) != null) {
                posWords.add(word);
            }

            // cleanup
            negReader.close();
            posReader.close();

            System.out.println("FINISH: reading file list");

            // ----------------------------------------------

            System.out.println("START: calculating sentiment");

            // current tweet
            Document tweet;
            // current score
            int score = 0;
            // current text
            String text;

            System.err.println("hello");
            //System.err.println(docReader.directory());

            // used to give some feedback during processing the 1 million tweets
            // do we want to skip saving that document?
            boolean skipSave = false;

            int i = 0;
            while ((text = reader.readLine()) != null) {
                if (i % 100000 == 0) {
                    System.out.printf("PROCESSING: %d tweets processed...%n", i);
                }

                // reset, most of the times we want that.
                skipSave = false;

                try {
                    if (text.startsWith("I'm at")
                            || text.startsWith("I just became the mayor")
                            || text.startsWith("I just ousted")) {
                        // all your foursquare updates are belong to us.
                        stats[4]++;
                        // and we don't save them. yo.
                        skipSave = true;
                    } else {
                        // finally! retrieve sentiment score.
                        score = getSentimentScore(text);
                        // ++ index so we won't have -1 and stuff...
                        stats[score + 1]++;

                        if (quiet)
                        {
                            String out = "";
                            if (score > 0)
                            {
                                out = "+1";
                            }
                            else if (score == 0)
                            {
                                out = "0";
                            }
                            else
                            {
                                out = "-1";
                            }
                            System.out.println(out);
                        }
                        else
                        {
                            System.out.printf("Score: %d for Tweet (%d): %s%n", score, i, text);
                        }
                    }
                } catch (Exception e) {
                    // something went wrong, ouuups!
                    e.printStackTrace();
                    System.err.printf("Error with tweet on line %d.%n", i);
                }
                ++i;
            }
            // cleanup
            reader.close();
            //fr.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.err.println(e1.getMessage());
        }

        System.out.println("FINISH: calculating sentiment");

        // ----------------------------------------------

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("----------------------------------------------");
        System.out.println("STATS - TIME: Analysis took "
                + TimeUnit.SECONDS.convert(totalTime, TimeUnit.MILLISECONDS)
                + " seconds");

        // ----------------------------------------------

        // get me some info!
        System.out.println("STATS - COUNTS: [negative | neutral | positive | not english | foursquare | no text to classify]");
        System.out.println("STATS - COUNTS: " + java.util.Arrays.toString(stats));
    }

    /**
     * does some string mangling and then calculates occurrences in positive /
     * negative word list and finally the delta
     *
     * @param input String: the text to classify
     * @return score int: if < 0 then -1, if > 0 then 1 otherwise 0 - we don't
     * care about the actual delta
     */
    private static int getSentimentScore(String input) {
        // normalize!
        input = input.toLowerCase();
        input = input.trim();
        // remove all non alpha-numeric non whitespace chars
        input = input.replaceAll("[^a-zA-Z0-9\\s]", "");

        int negCounter = 0;
        int posCounter = 0;

        // so what we got?
        String[] words = input.split(" ");

        // check if the current word appears in our reference lists...
        for (int i = 0; i < words.length; i++) {
            if (posWords.contains(words[i])) {
                posCounter++;
            }
            if (negWords.contains(words[i])) {
                negCounter++;
            }
        }

        // positive matches MINUS negative matches
        int result = (posCounter - negCounter);

        // negative?
        if (result < 0) {
            return -1;
            // or positive?
        } else if (result > 0) {
            return 1;
        }

        // neutral to the rescue!
        return 0;
    }

}