/*
    TunPRFAdap, language identifier using product of relative frequencies of character n-grams with adaptive language models
	See: Jauhiainen, T., Jauhiainen, H., & Linden, K. (2022). Italian Language and Dialect Identification and Regional French Variety Detection using Adaptive Naive Bayes. In Proceedings of the Ninth Workshop on NLP for Similar Languages, Varieties and Dialects (VarDial 2022). The Association for Computational Linguistics.
    
    Copyright 2020 Tommi Jauhiainen
	Copyright 2022 University of Helsinki
	Copyright 2022 Heidi Jauhiainen

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import java.io.*;
import java.util.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.lang.Math.*;

class TunPRFAdap {

// global tables representing the language models for all the languages
    
// gramDictCap = <character n-gram, language code, number found from training data>
	private static Table<String, String, Double> gramDictCap;
    
// typeAmount = <language code, length of character n-grams, number of character n-gram of that length in the language>
	private static Table<String, Integer, Double> typeAmounts;

    private static Map<Integer, String> identifiedLines = new LinkedHashMap<Integer, String>();

// global Maps holding the lines to be identified
// mysteryLines = <number of the line, the original text of the line>
    private static Map<Integer, String> mysteryLines;

//  The confidence score calculated by the identifier is reachable by the calling function as a global temporary variable "tempConfidence"

	private static Double tempConfidence = 0.0;
	
// Confidence threshold for actually adding data to models.

	private static double confidenceThreshold = 0.0;
	
// Function to flip the order of the map

	private static TreeMap<Double, ArrayList<Integer>> flipMap(LinkedHashMap<Integer, Double> thisMap) {
       TreeMap<Double, ArrayList<Integer>> newMap;
       ArrayList<Integer> list;
       int count = 0;
       newMap = new TreeMap<>(Collections.reverseOrder());
       for (Map.Entry<Integer, Double> entry : thisMap.entrySet()) {
               Integer linenumber = entry.getKey();
               Double number = entry.getValue();

               if (newMap.containsKey(number)) {
                   list = newMap.get(number);
                       list.add(linenumber);
               }
               else {
                   list = new ArrayList<>();
                   list.add(linenumber);
               }
               newMap.put(number, list);
       }
       return newMap;
   }

	public static void main(String[] args) {
	
		String trainFile = args[0];
		String testFile = args[1];
	
		gramDictCap = HashBasedTable.create();
		typeAmounts = HashBasedTable.create();
		
		List<String> languageList = new ArrayList<String>();
		mysteryLines = new LinkedHashMap<Integer, String>();

		
		int minCharNgram = 3;
		int maxCharNgram = 8;
		
		languageList = createModels(trainFile,minCharNgram,maxCharNgram);

		File file = new File(testFile);
		
		int x = minCharNgram;
		int y = maxCharNgram;
		double penaltymodifier = 2.1;
		
		evaluateFile(file,languageList,x,y,penaltymodifier);
	}

	private static void evaluateFile(File file, List<String> languageList, int minCharNgram, int maxCharNgram, double penaltymodifier) {
		BufferedReader testfilereader = null;
		File testfile = file;
		int totallinenumber = 1;
		try {
			testfilereader = new BufferedReader(new FileReader(testfile));
			String testline = "";
			while ((testline = testfilereader.readLine()) != null) {
				mysteryLines.put(totallinenumber,testline);
				totallinenumber++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (testfilereader != null) {
					testfilereader.close();
				}
			} catch (IOException e) {
			}
		}
		totallinenumber--;

		ListIterator gramiterator = languageList.listIterator();

		int epoch = 1;
		int numberofsplits = 512;
//		int numberofsplits = totallinenumber;
		while (epoch < 2) {
	
			Map<String, Integer> langCorrect;
			Map<String, Integer> langWrong;
			Map<String, Integer> langShouldBe;

			langCorrect = new LinkedHashMap<String, Integer>();
			langWrong = new LinkedHashMap<String, Integer>();
			langShouldBe = new LinkedHashMap<String, Integer>();

			int langamount = 0;
			gramiterator = languageList.listIterator();
			while(gramiterator.hasNext()) {
				langamount++;
				Object element = gramiterator.next();
				String language = (String) element;
				langShouldBe.put(language,0);
				langCorrect.put(language,0);
				langWrong.put(language,0);
			}

			int counter = 0;

			float correct = 0;
			float wrong = 0;
			float total = 0;
            
            identifiedLines = new LinkedHashMap<Integer, String>();
			
			while (counter < totallinenumber) {
				int linenumber = 1;

// linescores has the line and the difference between the best and the second scored language

				LinkedHashMap<Integer, Double> lineScores = new LinkedHashMap<Integer, Double>();
				
// tempIdentifiedLines has the identification from the original round

				Map<Integer, String> tempIdentifiedLines = new LinkedHashMap<Integer, String>();

				while (linenumber <= totallinenumber) {
					if (!identifiedLines.containsKey(linenumber)) {
						String testline = mysteryLines.get(linenumber);
												
						String mysterytext = testline;
	//					String correctlanguage = testline;

			// if the correct language is tab separated then remove it from the mystery text
                        
                        mysterytext = mysterytext.replaceAll(".*\t", "");
						
//                        mysterytext = mysterytext.replaceAll("[^\\p{L}\\p{M}′'’´ʹािीुूृेैोौंँः् া ি ী ু ূ ৃ ে ৈ ো ৌ।্্্я̄\\u07A6\\u07A7\\u07A8\\u07A9\\u07AA\\u07AB\\u07AC\\u07AD\\u07AE\\u07AF\\u07B0\\u0A81\\u0A82\\u0A83\\u0ABC\\u0ABD\\u0ABE\\u0ABF\\u0AC0\\u0AC1\\u0AC2\\u0AC3\\u0AC4\\u0AC5\\u0AC6\\u0AC7\\u0AC8\\u0AC9\\u0ACA\\u0ACB\\u0ACC\\u0ACD\\u0AD0\\u0AE0\\u0AE1\\u0AE2\\u0AE3\\u0AE4\\u0AE5\\u0AE6\\u0AE7\\u0AE8\\u0AE9\\u0AEA\\u0AEB\\u0AEC\\u0AED\\u0AEE\\u0AEF\\u0AF0\\u0AF1]", " ");
                        mysterytext = mysterytext.replaceAll("  *", " ");
                        mysterytext = mysterytext.replaceAll("^ ", "");
                        mysterytext = mysterytext.replaceAll(" $", "");
						mysterytext = mysterytext.replaceAll("^", " ");
						mysterytext = mysterytext.replaceAll("$", " ");
   //                     mysterytext = mysterytext.toLowerCase();
	/*
						correctlanguage = correctlanguage.replaceAll(".*\t", "");
						correctlanguage = correctlanguage.replaceAll("\n", "");
	*/
						String identifiedLanguage = identifyTextProdRelFreq(mysterytext,languageList,minCharNgram,maxCharNgram,penaltymodifier);
						lineScores.put(linenumber,tempConfidence);
						tempIdentifiedLines.put(linenumber,identifiedLanguage);
					}
					linenumber++;
				}

				TreeMap<Double, ArrayList<Integer>> arrangedScores = new TreeMap<Double, ArrayList<Integer>>();
				arrangedScores = flipMap(lineScores);
				
				int montakoidentifioidaan = totallinenumber/numberofsplits;
				int laskuri = 1;
				double lowestConfidence = 0.0;

				for (Map.Entry<Double, ArrayList<Integer>> entry : arrangedScores.entrySet()) {
					ArrayList<Integer> linenumberList = entry.getValue();
					Double scoreDifference = entry.getKey();
					for (int number : linenumberList) {
						counter++;
						
						String texttoadd = mysteryLines.get(number);
//						String correctlanguage = texttoadd;
//						correctlanguage = correctlanguage.replaceAll(".*\t", "");
//						correctlanguage = correctlanguage.replaceAll("\n", "");
						texttoadd = texttoadd.replaceAll("\t.*", "");
						String thelanguage = tempIdentifiedLines.get(number);
				
						identifiedLines.put(number,thelanguage);
						if (!thelanguage.equals("xxx")) {
							if (lineScores.get(number) > confidenceThreshold) {
// quote out the next line if you want results without LM adaptation
								updatemodel(texttoadd,thelanguage,minCharNgram,maxCharNgram);
							}
						}

//-------- quote from here this out produce final predictions ->
                        /*
			// Here we calculate the accuracy
						if (!correctlanguage.equals("XY")) {
							total++;
							langShouldBe.put(correctlanguage,langShouldBe.get(correctlanguage)+1);
							
							if (!thelanguage.equals("xxx")) {
								if (thelanguage.equals(correctlanguage)) {
									correct++;
									langCorrect.put(thelanguage,langCorrect.get(thelanguage)+1);
								}
								else {
									wrong++;
									langWrong.put(thelanguage,langWrong.get(thelanguage)+1);
								}
							}
						}
                         */
//-------- quote from here this out produce final predictions <-
						lowestConfidence = lineScores.get(number);

						laskuri++;
					}
					
					if (laskuri > montakoidentifioidaan) {
//						System.out.println("Accuracy: " + (100/(correct + wrong)*correct) + " Total: " + (correct + wrong) + " Correct: " + correct + " Wrong: " + wrong + " Lowest confidence: " + lowestConfidence);
						break;
					}
			   	}

			}
			/*
			Float sumFscore = (float)0;
			Float sumWeightedFscore = (float)0;

			gramiterator = languageList.listIterator();
			while(gramiterator.hasNext()) {
				Object element = gramiterator.next();
				String language = (String) element;
				Float precision = (float)langCorrect.get(language) / (float)(langCorrect.get(language) + (float)langWrong.get(language));
                if (langCorrect.get(language) == 0) {
                    precision = (float)0;
                }
				Float recall = (float)langCorrect.get(language) / (float)langShouldBe.get(language);
				Float f1score = 2*(precision*recall/(precision+recall));
                if (precision+recall == 0) {
                    f1score = (float)0;
                }
				sumFscore = sumFscore + f1score;
//				System.out.println("Precision: " + precision + "\tRecall: " + recall + "\t" + sumWeightedFscore + "\t" + f1score  + "\t" + langCorrect.get(language)  + "\t" + langShouldBe.get(language));
				sumWeightedFscore = sumWeightedFscore + (f1score * (float)langShouldBe.get(language) / (correct + wrong));
			}
			
			Float macroF1Score = sumFscore / (float)langamount;
//          System.out.println(sumFscore + " " + (float)langamount);
			Float weightedF1Score = sumWeightedFscore;
//			System.out.println((correct + wrong));

			Float totalPrecision = correct / (correct + wrong);
			Float totalRecall = correct / (correct + wrong);

			Float microF1Score = 2*(totalPrecision*totalRecall/(totalPrecision+totalRecall));
			
//			System.out.println(epoch + " " + macroF1Score + " " + 100.0/total*correct);

//			System.out.println(penaltymodifier + " " + macroF1Score + " " + 100.0/total*correct);

//			System.out.println(numberofsplits + " " + macroF1Score + " " + 100.0/total*correct);
			
//			System.out.println(epoch + " " + macroF1Score + "\t" + 100.0/total*correct + "\t" + microF1Score + "\t" + weightedF1Score);
//			System.out.println("E: " + epoch + " CT: " + confidenceThreshold + " NS: " + numberofsplits + " MacroF1: " + macroF1Score + "\t" + 100.0/total*correct + "\t MicroF1: " + microF1Score + "\t" + weightedF1Score);
             */
			epoch = epoch + 1;
		}
        // To print out final identification
        int laskuri = 1;
        while (laskuri < (totallinenumber+1)) {
            if (identifiedLines.get(laskuri).equals("xxx")) {
                identifiedLines.put(laskuri,"oth");
            }
            System.out.println(identifiedLines.get(laskuri));
            laskuri++;
        }
    }
    
// identifyText
	
	private static String identifyTextProdRelFreq(String mysteryText, List<String> languageList, int minCharNgram, int maxCharNgram, double penaltymodifier) {
		
		Map<String, Double> languagescores = new HashMap();

		ListIterator languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			languagescores.put(kieli, 0.0);
		}

		int t = maxCharNgram;
		int gramamount = 0;

		while (t >= minCharNgram) {
			int pituus = mysteryText.length();
			int x = 0;
			if (pituus > (t-1)) {
				while (x < pituus - t + 1) {
					String gram = mysteryText.substring(x,x+t);
					gramamount = gramamount + 1;
					languageiterator = languageList.listIterator();
					while(languageiterator.hasNext()) {
						Object element = languageiterator.next();
						String kieli = (String) element;
						if (gramDictCap.contains(gram,kieli)) {
							double probability = -Math.log10(gramDictCap.get(gram,kieli) / (typeAmounts.get(kieli,t)));
							languagescores.put(kieli, languagescores.get(kieli) +probability);
						}
						else {
							double penalty = -Math.log10(1/typeAmounts.get(kieli,t))*penaltymodifier;
							languagescores.put(kieli, languagescores.get(kieli) +penalty);
						}
					}
					x = x + 1;
				}
			}
			t = t -1 ;
		}
		
		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String language = (String) element;
			languagescores.put(language, (languagescores.get(language)/gramamount));
		}

		Double winningscore = 1000.0;
		String mysterylanguage = "xxx";

		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String kieli = (String) element;
			if (languagescores.get(element) < winningscore) {
				winningscore = languagescores.get(element);
				mysterylanguage = kieli;
			}
		}
		
		Double secondscore = 1000.0;
		String secondlanguage = "xxx";
		
		languageiterator = languageList.listIterator();
		while(languageiterator.hasNext()) {
			Object element = languageiterator.next();
			String language = (String) element;
			if (language != mysterylanguage) {
				if (languagescores.get(element) < secondscore) {
					secondscore = languagescores.get(element);
					secondlanguage = language;
				}
			}
		}
		tempConfidence = secondscore - winningscore;
			
		return (mysterylanguage);
	}
	
	private static List createModels(String trainFile, int minCharNgram, int maxCharNgram) {
	
		List<String> languageList = new ArrayList<String>();
	
		File file = new File(trainFile);
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = "";
			
			while ((line = reader.readLine()) != null) {
				String text = line;
				String language = line;
                
                text = text.replaceAll(".*\t", "");
				
//                text = text.replaceAll("[^\\p{L}\\p{M}′'’´ʹािीुूृेैोौंँः् া ি ী ু ূ ৃ ে ৈ ো ৌ।্্্я̄\\u07A6\\u07A7\\u07A8\\u07A9\\u07AA\\u07AB\\u07AC\\u07AD\\u07AE\\u07AF\\u07B0\\u0A81\\u0A82\\u0A83\\u0ABC\\u0ABD\\u0ABE\\u0ABF\\u0AC0\\u0AC1\\u0AC2\\u0AC3\\u0AC4\\u0AC5\\u0AC6\\u0AC7\\u0AC8\\u0AC9\\u0ACA\\u0ACB\\u0ACC\\u0ACD\\u0AD0\\u0AE0\\u0AE1\\u0AE2\\u0AE3\\u0AE4\\u0AE5\\u0AE6\\u0AE7\\u0AE8\\u0AE9\\u0AEA\\u0AEB\\u0AEC\\u0AED\\u0AEE\\u0AEF\\u0AF0\\u0AF1]", " ");
                text = text.replaceAll("  *", " ");
                text = text.replaceAll("^ ", "");
                text = text.replaceAll(" $", "");
 //               text = text.toLowerCase();
                
                int pituus = text.length();
                
				language = language.replaceAll("\t.*", "");
				language = language.replaceAll("\n", "");
				
				if (!languageList.contains(language)) {
					languageList.add(language);
					int x = maxCharNgram;
					while (x >= minCharNgram) {
						typeAmounts.put(language,x,0.0);
						x--;
					}
				}
				
				int t = maxCharNgram;
				
				while (t >= minCharNgram) {
					
					int x = 0;
					if (pituus > (t-1)) {
						while (x < pituus - t + 1) {
							String gram = text.substring(x,x+t);
							if (gramDictCap.contains(gram,language)) {
								gramDictCap.put(gram, language, gramDictCap.get(gram,language) + 1);
							}
							else {
								gramDictCap.put(gram,language, 1.0);
							}
							typeAmounts.put(language,t,typeAmounts.get(language,t)+1);
							x = x + 1;
						}
					}
					t = t -1 ;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
		return (languageList);
	}
	
	private static void updatemodel(String line, String language, int minCharNgram, int maxCharNgram) {
	
		String text = line;

		int t = maxCharNgram;

		while (t >= minCharNgram) {
			int pituus = text.length();
			int x = 0;
			if (pituus > (t-1)) {
				while (x < pituus - t + 1) {
					String gram = text.substring(x,x+t);
					if (gramDictCap.contains(gram,language)) {
						gramDictCap.put(gram, language, gramDictCap.get(gram,language) + 1);
					}
					else {
						gramDictCap.put(gram,language, 1.0);
					}
					typeAmounts.put(language,t,typeAmounts.get(language,t)+1);
					x = x + 1;
				}
			}
			t = t -1 ;
		}
	}
}
