package cto.hmi.processor.nlu.soda.classification.features;


public class InterrogativeFeature extends Feature{
	
	//HashSet<String> condWords=new HashSet<String>(Arrays.asList("could", "should"));
	
	public InterrogativeFeature() {
		super("interrogative"); //in this case we mean questions
	}

	@Override
	protected boolean hasFeature(String utterance) {
		
		//if second word is you or is or I
		//can YOU tell me ...?
		//what/how IS ...? 
		String[] words=utterance.split(" ");
		if(words.length>1){
			return (
					//(words[1].equals("you") || words[1].equals("is") || words[1].equals("i")) || 
					(words[0].toLowerCase().equals("do") || /*words[0].equals("can")) ||*/
					words[0].toLowerCase().equals("is")||
					words[0].equals("क्या")) //05.07.2015, added
					);
		}
		else return false;
	}

}
